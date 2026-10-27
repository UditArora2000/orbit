import SwiftData
import SwiftUI

/// The shared "fill out this person" form, used by both the triage flow and the
/// person detail screen. Recreate it with .id(person.persistentModelID) so field
/// state resets when a different person comes in.
struct PersonEditorView: View {
    let person: Person
    let saveLabel: String
    var onSave: () -> Void = {}

    @Environment(\.modelContext) private var context
    @State private var how: String
    @State private var relationship: String
    @State private var notes: String
    @State private var canHangout: Bool
    @State private var canCall: Bool
    @State private var isTight: Bool

    init(person: Person, saveLabel: String, onSave: @escaping () -> Void = {}) {
        self.person = person
        self.saveLabel = saveLabel
        self.onSave = onSave
        _how = State(initialValue: person.howWeKnow)
        _relationship = State(initialValue: person.relationship)
        _notes = State(initialValue: person.notes)
        _canHangout = State(initialValue: person.canHangout)
        _canCall = State(initialValue: person.canCall)
        _isTight = State(initialValue: person.isTight)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                AvatarView(person: person, size: 56)
                VStack(alignment: .leading) {
                    Text(person.displayName).font(.title3.bold())
                    if let phone = person.phoneNumber {
                        Text(phone).font(.caption).foregroundStyle(.secondary)
                    }
                    if let ig = person.instagramUsername {
                        Text("@\(ig) on Instagram").font(.caption).foregroundStyle(.secondary)
                    }
                }
            }

            TextField("How do you know them?", text: $how, axis: .vertical)
                .textFieldStyle(.roundedBorder)

            Text("Relationship").font(.caption).foregroundStyle(.secondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Person.relationshipOptions, id: \.self) { option in
                        Chip(label: option, selected: relationship == option) {
                            relationship = relationship == option ? "" : option
                        }
                    }
                }
            }

            Toggle("Can I hang out with them?", isOn: $canHangout)
            Toggle(isOn: $canCall) {
                VStack(alignment: .leading) {
                    Text("Can I call them?")
                    Text("They'll go into your call rotation")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Toggle(isOn: $isTight) {
                VStack(alignment: .leading) {
                    Text("Are we tight?")
                    Text("Your inner circle")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            TextField("Notes (optional)", text: $notes, axis: .vertical)
                .lineLimit(2...4)
                .textFieldStyle(.roundedBorder)

            HStack {
                Spacer()
                Button(saveLabel) { save() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .card()
    }

    private func save() {
        person.howWeKnow = how.trimmingCharacters(in: .whitespacesAndNewlines)
        person.relationship = relationship
        person.notes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        person.canHangout = canHangout
        person.canCall = canCall
        person.isTight = isTight
        person.status = Person.statusTriaged
        person.updatedAt = .now
        try? context.save()
        onSave()
    }
}
