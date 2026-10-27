import Foundation
import SwiftData

@Model
final class Person {
    // Link back to the CNContact, when this person came from the address book.
    var contactIdentifier: String?
    var displayName: String
    var phoneNumber: String?
    var photoData: Data?
    var instagramUsername: String?
    var source: String
    var howWeKnow: String
    var relationship: String
    var notes: String
    var canHangout: Bool
    var canCall: Bool
    var isTight: Bool = false
    var status: String
    var skipCount: Int
    var lastContactedAt: Date?
    var contactedThisCycle: Bool
    var createdAt: Date
    var updatedAt: Date

    init(
        contactIdentifier: String? = nil,
        displayName: String,
        phoneNumber: String? = nil,
        photoData: Data? = nil,
        instagramUsername: String? = nil,
        source: String = Person.sourceContacts
    ) {
        self.contactIdentifier = contactIdentifier
        self.displayName = displayName
        self.phoneNumber = phoneNumber
        self.photoData = photoData
        self.instagramUsername = instagramUsername
        self.source = source
        self.howWeKnow = ""
        self.relationship = ""
        self.notes = ""
        self.canHangout = false
        self.canCall = false
        self.isTight = false
        self.status = Person.statusUntriaged
        self.skipCount = 0
        self.lastContactedAt = nil
        self.contactedThisCycle = false
        self.createdAt = .now
        self.updatedAt = .now
    }

    static let sourceContacts = "CONTACTS"
    static let sourceInstagram = "INSTAGRAM"
    static let statusUntriaged = "UNTRIAGED"
    static let statusTriaged = "TRIAGED"
    static let statusArchived = "ARCHIVED"
    static let relationshipOptions = [
        "Friend", "Family", "Work", "School", "Neighbor", "Acquaintance", "Other",
    ]
}

@Model
final class CatchUpLog {
    var personName: String
    var timestamp: Date

    init(personName: String, timestamp: Date = .now) {
        self.personName = personName
        self.timestamp = timestamp
    }
}

/// Lowercased letters only, used to fuzzy-match Instagram usernames against contact names.
func normalizeName(_ name: String) -> String {
    name.lowercased().filter { $0.isLetter }
}

enum Rotation {
    /// Can-call friends not yet contacted this round, most-overdue first
    /// (never-contacted people come before everyone else).
    static func candidates(_ people: [Person]) -> [Person] {
        people
            .filter { $0.status == Person.statusTriaged && $0.canCall && !$0.contactedThisCycle }
            .sorted { a, b in
                switch (a.lastContactedAt, b.lastContactedAt) {
                case (nil, nil):
                    return a.displayName.localizedCaseInsensitiveCompare(b.displayName) == .orderedAscending
                case (nil, _):
                    return true
                case (_, nil):
                    return false
                case let (x?, y?):
                    return x < y
                }
            }
    }

    static func markCaughtUp(_ person: Person, context: ModelContext) {
        person.lastContactedAt = .now
        person.contactedThisCycle = true
        person.updatedAt = .now
        context.insert(CatchUpLog(personName: person.displayName))
        try? context.save()
    }
}
