import Contacts
import Foundation
import SwiftData

/// Pulls the phone's contacts into the local store. New contacts land in the triage
/// queue; existing ones get their name/number/photo refreshed. If an Instagram-only
/// person matches a new contact by name, the two records merge instead of duplicating.
enum ContactsSyncer {
    struct SyncResult {
        let newCount: Int
        let updatedCount: Int
    }

    static var isAuthorized: Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        if #available(iOS 18.0, *), status == .limited { return true }
        return status == .authorized
    }

    static func requestAccess() async -> Bool {
        if isAuthorized { return true }
        let store = CNContactStore()
        let granted = (try? await store.requestAccess(for: .contacts)) ?? false
        return granted || isAuthorized
    }

    @discardableResult
    static func sync(context: ModelContext) -> SyncResult {
        guard isAuthorized else { return SyncResult(newCount: 0, updatedCount: 0) }
        let store = CNContactStore()
        let keys: [CNKeyDescriptor] = [
            CNContactIdentifierKey as CNKeyDescriptor,
            CNContactOrganizationNameKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor,
            CNContactThumbnailImageDataKey as CNKeyDescriptor,
            CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
        ]

        struct Row {
            let id: String
            let name: String
            let phone: String?
            let photo: Data?
        }

        var rows: [Row] = []
        let request = CNContactFetchRequest(keysToFetch: keys)
        do {
            try store.enumerateContacts(with: request) { contact, _ in
                let formatted = CNContactFormatter.string(from: contact, style: .fullName)?
                    .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                let name = formatted.isEmpty
                    ? contact.organizationName.trimmingCharacters(in: .whitespacesAndNewlines)
                    : formatted
                guard !name.isEmpty else { return }
                rows.append(Row(
                    id: contact.identifier,
                    name: name,
                    phone: contact.phoneNumbers.first?.value.stringValue,
                    photo: contact.thumbnailImageData
                ))
            }
        } catch {
            return SyncResult(newCount: 0, updatedCount: 0)
        }

        let people = (try? context.fetch(FetchDescriptor<Person>())) ?? []
        var byIdentifier: [String: Person] = [:]
        var instagramByName: [String: Person] = [:]
        for p in people {
            if let id = p.contactIdentifier {
                byIdentifier[id] = p
            } else if instagramByName[normalizeName(p.displayName)] == nil {
                instagramByName[normalizeName(p.displayName)] = p
            }
        }

        var newCount = 0
        var updatedCount = 0
        for row in rows {
            if let existing = byIdentifier[row.id] {
                var changed = false
                if existing.displayName != row.name {
                    existing.displayName = row.name
                    changed = true
                }
                if let phone = row.phone, existing.phoneNumber != phone {
                    existing.phoneNumber = phone
                    changed = true
                }
                if let photo = row.photo, existing.photoData != photo {
                    existing.photoData = photo
                    changed = true
                }
                if changed {
                    existing.updatedAt = .now
                    updatedCount += 1
                }
                continue
            }
            let normalized = normalizeName(row.name)
            if !normalized.isEmpty, let match = instagramByName.removeValue(forKey: normalized) {
                match.contactIdentifier = row.id
                match.displayName = row.name
                match.phoneNumber = row.phone
                match.photoData = row.photo
                match.source = Person.sourceContacts
                match.updatedAt = .now
                updatedCount += 1
            } else {
                context.insert(Person(
                    contactIdentifier: row.id,
                    displayName: row.name,
                    phoneNumber: row.phone,
                    photoData: row.photo
                ))
                newCount += 1
            }
        }

        try? context.save()
        return SyncResult(newCount: newCount, updatedCount: updatedCount)
    }
}
