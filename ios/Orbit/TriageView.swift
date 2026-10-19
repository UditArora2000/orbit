import SwiftData
import SwiftUI

struct TriageView: View {
    @Environment(\.modelContext) private var context
    @Query private var people: [Person]

    private var queue: [Person] {
        people.filter { $0.status == Person.statusUntriaged }
            .sorted {
                if $0.skipCount != $1.skipCount { return $0.skipCount < $1.skipCount }
                return $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
            }
    }

    private var activeCount: Int { people.filter { $0.status != Person.statusArchived }.count }

    private var skippedCount: Int {
        people.filter { $0.status == Person.statusUntriaged && $0.skipCount > 0 }.count
    }

    private var archivedCount: Int {
        people.filter { $0.status == Person.statusArchived }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("One at a time: say how you know them and whether they're call or hangout material.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    let filed = activeCount - queue.count
                    LabeledProgress(done: filed, total: activeCount, caption: "\(filed) of \(activeCount) filed")

                    if skippedCount > 0 || archivedCount > 0 {
                        HStack(spacing: 8) {
                            if skippedCount > 0 {
                                NavigationLink {
                                    ReviewView(mode: .skipped)
                                } label: {
                                    Text("Skipped (\(skippedCount))")
                                }
                                .buttonStyle(.bordered)
                            }
                            if archivedCount > 0 {
                                NavigationLink {
                                    ReviewView(mode: .archived)
                                } label: {
                                    Text("Archived (\(archivedCount))")
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }

                    if let person = queue.first {
                        PersonEditorView(person: person, saveLabel: "Done — next")
                            .id(person.persistentModelID)
                        HStack {
                            Button("Skip") {
                                person.skipCount += 1
                                person.updatedAt = .now
                                try? context.save()
                            }
                            .buttonStyle(.bordered)
                            Button("Archive", role: .destructive) {
                                person.status = Person.statusArchived
                                person.updatedAt = .now
                                try? context.save()
                            }
                            .buttonStyle(.bordered)
                        }
                        Text("Skip sends them to the back of the queue. Archive is for businesses and numbers that aren't really people.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("All caught up").font(.title3.bold())
                            Text("When you save a new contact it shows up here automatically after a sync.")
                                .foregroundStyle(.secondary)
                        }
                        .card()
                    }
                }
                .padding()
            }
            .navigationTitle("File your people")
        }
    }
}
