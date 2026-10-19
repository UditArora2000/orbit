import Foundation
import SwiftData

/// Imports the accounts you follow from Instagram's "Download your information"
/// export (the `following.json` file). Instagram shut down API access to personal
/// follow lists, so the data export file is the only reliable way in.
enum InstagramImporter {
    struct ImportResult {
        let linked: Int
        let created: Int
        let skipped: Int
    }

    static func importFollowing(from url: URL, context: ModelContext) -> ImportResult {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        guard let data = try? Data(contentsOf: url) else {
            return ImportResult(linked: 0, created: 0, skipped: 0)
        }
        let usernames = parse(data)

        let people = (try? context.fetch(FetchDescriptor<Person>())) ?? []
        let alreadyLinked = Set(people.compactMap { $0.instagramUsername?.lowercased() })
        var byName: [String: Person] = [:]
        for p in people {
            let key = normalizeName(p.displayName)
            if byName[key] == nil { byName[key] = p }
        }

        var linked = 0
        var created = 0
        var skipped = 0
        for username in usernames {
            if alreadyLinked.contains(username.lowercased()) {
                skipped += 1
                continue
            }
            if let match = byName[normalizeName(username)] {
                if match.instagramUsername == nil {
                    match.instagramUsername = username
                    match.updatedAt = .now
                    linked += 1
                } else {
                    skipped += 1
                }
            } else {
                context.insert(Person(
                    displayName: username,
                    instagramUsername: username,
                    source: Person.sourceInstagram
                ))
                created += 1
            }
        }

        try? context.save()
        return ImportResult(linked: linked, created: created, skipped: skipped)
    }

    /// Handles both the object form ({"relationships_following": [...]}) and a bare array.
    static func parse(_ data: Data) -> [String] {
        guard let root = try? JSONSerialization.jsonObject(with: data) else { return [] }
        let array: [Any]
        if let a = root as? [Any] {
            array = a
        } else if let obj = root as? [String: Any] {
            array = (obj["relationships_following"] as? [Any])
                ?? obj.values.compactMap { $0 as? [Any] }.first
                ?? []
        } else {
            return []
        }

        var seen = Set<String>()
        var result: [String] = []
        for element in array {
            guard let obj = element as? [String: Any] else { continue }
            let fromList = ((obj["string_list_data"] as? [Any])?.first as? [String: Any])?["value"] as? String
            let title = obj["title"] as? String
            guard let username = (fromList ?? title)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !username.isEmpty,
                  seen.insert(username).inserted
            else { continue }
            result.append(username)
        }
        return result
    }
}
