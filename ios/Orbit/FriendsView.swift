import SwiftData
import SwiftUI

struct FriendsView: View {
    enum Filter: String, CaseIterable, Identifiable {
        case all = "All"
        case call = "Can call"
        case hangout = "Can hang out"
        var id: String { rawValue }
    }

    @Query private var people: [Person]
    @State private var search = ""
    @State private var filter: Filter = .all

    private var friends: [Person] {
        people
            .filter { $0.status == Person.statusTriaged }
            .filter { person in
                switch filter {
                case .all: return true
                case .call: return person.canCall
                case .hangout: return person.canHangout
                }
            }
            .filter { person in
                if search.isEmpty { return true }
                return person.displayName.localizedCaseInsensitiveContains(search)
                    || person.howWeKnow.localizedCaseInsensitiveContains(search)
                    || (person.instagramUsername ?? "").localizedCaseInsensitiveContains(search)
            }
            .sorted { $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending }
    }

    var body: some View {
        NavigationStack {
            List {
                Picker("Filter", selection: $filter) {
                    ForEach(Filter.allCases) { f in
                        Text(f.rawValue).tag(f)
                    }
                }
                .pickerStyle(.segmented)
                .listRowSeparator(.hidden)

                ForEach(friends) { person in
                    NavigationLink {
                        PersonDetailView(person: person)
                    } label: {
                        FriendRow(person: person)
                    }
                }

                if friends.isEmpty {
                    Text("No one here yet — file people first, then they show up in this list.")
                        .foregroundStyle(.secondary)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .searchable(text: $search)
            .navigationTitle("Friends")
        }
    }
}

struct FriendRow: View {
    @Environment(\.modelContext) private var context
    @Environment(\.openURL) private var openURL
    let person: Person

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(person: person)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(person.displayName).font(.headline)
                    if person.contactedThisCycle {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(Color.accentColor)
                    }
                }
                let subtitle = [person.relationship, person.howWeKnow]
                    .filter { !$0.isEmpty }
                    .joined(separator: " · ")
                if !subtitle.isEmpty {
                    Text(subtitle).font(.caption).foregroundStyle(.secondary)
                }
                Text(person.lastContactedAt.map { "Last caught up \(relativeTime($0))" } ?? "No catch-ups yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if let url = telURL(person.phoneNumber) {
                Button {
                    openURL(url)
                } label: {
                    Image(systemName: "phone")
                }
                .buttonStyle(.borderless)
            } else if let url = instagramURL(person.instagramUsername) {
                Button {
                    openURL(url)
                } label: {
                    Image(systemName: "camera")
                }
                .buttonStyle(.borderless)
            }
            Button {
                Rotation.markCaughtUp(person, context: context)
            } label: {
                Image(systemName: "checkmark.circle")
            }
            .buttonStyle(.borderless)
        }
    }
}

struct PersonDetailView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.openURL) private var openURL
    @Environment(\.dismiss) private var dismiss
    let person: Person

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                PersonEditorView(person: person, saveLabel: "Save") { dismiss() }
                HStack {
                    if let url = telURL(person.phoneNumber) {
                        Button {
                            openURL(url)
                        } label: {
                            Label("Call", systemImage: "phone")
                        }
                        .buttonStyle(.borderedProminent)
                    } else if let url = instagramURL(person.instagramUsername) {
                        Button("Open Instagram") { openURL(url) }
                            .buttonStyle(.borderedProminent)
                    }
                    Button("Caught up ✓") {
                        Rotation.markCaughtUp(person, context: context)
                    }
                    .buttonStyle(.bordered)
                }
                Text(person.lastContactedAt.map { "Last caught up \(relativeTime($0))" } ?? "No catch-ups logged yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button("Archive this person", role: .destructive) {
                    person.status = Person.statusArchived
                    person.updatedAt = .now
                    try? context.save()
                    dismiss()
                }
            }
            .padding()
        }
        .navigationTitle(person.displayName)
        .navigationBarTitleDisplayMode(.inline)
    }
}
