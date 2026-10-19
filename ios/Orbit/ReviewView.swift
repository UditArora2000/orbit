import SwiftData
import SwiftUI

/// The "Skipped" and "Archived" lists reachable from the filing screen, so
/// nobody ever looks lost forever. Skipped people can be bumped to the front
/// of the queue; archived people can be restored into it.
struct ReviewView: View {
    enum Mode {
        case skipped
        case archived
    }

    let mode: Mode

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query private var people: [Person]

    private var listed: [Person] {
        switch mode {
        case .skipped:
            return people
                .filter { $0.status == Person.statusUntriaged && $0.skipCount > 0 }
                .sorted {
                    if $0.skipCount != $1.skipCount { return $0.skipCount < $1.skipCount }
                    return $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
                }
        case .archived:
            return people
                .filter { $0.status == Person.statusArchived }
                .sorted { $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending }
        }
    }

    var body: some View {
        List {
            Text(
                mode == .skipped
                    ? "Skipped people are still in your filing queue — they come back around after everyone else. Tap one to fill them out now, or move them to the front."
                    : "Archived people are hidden from filing and your friends list (meant for businesses and numbers that aren't people). Restore someone to put them back in the queue."
            )
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .listRowSeparator(.hidden)

            ForEach(listed) { person in
                NavigationLink {
                    PersonDetailView(person: person)
                } label: {
                    row(for: person)
                }
            }

            if listed.isEmpty {
                Text(mode == .skipped ? "Nothing skipped right now." : "Nothing archived right now.")
                    .foregroundStyle(.secondary)
                    .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .navigationTitle(mode == .skipped ? "Skipped" : "Archived")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func row(for person: Person) -> some View {
        HStack(spacing: 12) {
            AvatarView(person: person)
            VStack(alignment: .leading, spacing: 2) {
                Text(person.displayName).font(.headline)
                if mode == .skipped {
                    Text(person.skipCount == 1 ? "Skipped once" : "Skipped \(person.skipCount) times")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else if let phone = person.phoneNumber, !phone.isEmpty {
                    Text(phone).font(.caption).foregroundStyle(.secondary)
                }
            }
            Spacer()
            if mode == .skipped {
                Button("File next") {
                    bringToFront(person)
                    dismiss()
                }
                .buttonStyle(.bordered)
            } else {
                Button("Restore") {
                    restore(person)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    /// Queue ordering is skipCount ascending, so one less than the current
    /// minimum puts this person ahead of everyone.
    private func bringToFront(_ person: Person) {
        let minSkip = people
            .filter { $0.status == Person.statusUntriaged }
            .map(\.skipCount)
            .min() ?? 0
        person.skipCount = minSkip - 1
        person.updatedAt = .now
        try? context.save()
    }

    private func restore(_ person: Person) {
        person.status = Person.statusUntriaged
        person.skipCount = 0
        person.updatedAt = .now
        try? context.save()
    }
}
