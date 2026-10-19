import SwiftData
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @Environment(\.modelContext) private var context
    @Query private var people: [Person]
    @AppStorage("nudgeEnabled") private var nudgeEnabled = true
    @AppStorage("nudgeHour") private var nudgeHour = 18
    @AppStorage("nudgeMinute") private var nudgeMinute = 0
    @State private var showImporter = false
    @State private var message: String?

    private var nudgeTime: Binding<Date> {
        Binding(
            get: {
                Calendar.current.date(
                    bySettingHour: nudgeHour, minute: nudgeMinute, second: 0, of: .now
                ) ?? .now
            },
            set: { newValue in
                let comps = Calendar.current.dateComponents([.hour, .minute], from: newValue)
                nudgeHour = comps.hour ?? 18
                nudgeMinute = comps.minute ?? 0
                NudgeScheduler.reschedule(people: people)
                message = "Daily nudge updated"
            }
        )
    }

    var body: some View {
        NavigationStack {
            Form {
                if let message {
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(Color.accentColor)
                }

                Section {
                    Toggle("Nudge me daily", isOn: $nudgeEnabled)
                        .onChange(of: nudgeEnabled) { _, _ in
                            NudgeScheduler.reschedule(people: people)
                        }
                    DatePicker("Nudge time", selection: nudgeTime, displayedComponents: .hourAndMinute)
                        .disabled(!nudgeEnabled)
                } header: {
                    Text("Daily nudge")
                } footer: {
                    Text("Once a day, Orbit picks a friend from your call rotation and reminds you to reach out.")
                }

                Section {
                    Button("Sync now") {
                        let result = ContactsSyncer.sync(context: context)
                        message = "Sync done: \(result.newCount) new, \(result.updatedCount) updated"
                        NudgeScheduler.reschedule(people: people)
                    }
                } header: {
                    Text("Contacts")
                } footer: {
                    Text("Contacts re-sync every time you open Orbit (and occasionally in the background, when iOS allows it). New people get queued for filing.")
                }

                Section {
                    Button("Import following.json") { showImporter = true }
                } header: {
                    Text("Instagram")
                } footer: {
                    Text("""
                    Instagram doesn't let apps read your follow list directly, but you can import it from your data export:

                    1. Instagram → profile → menu → Accounts Center
                    2. Your information and permissions → Download your information
                    3. Request a JSON export of "Followers and following"
                    4. Pick the following.json file here

                    Accounts that match a contact's name get linked; the rest are added as new people to file.
                    """)
                }

                Section {
                } footer: {
                    Text("Orbit keeps everything on your phone — nothing is uploaded anywhere.")
                }
            }
            .navigationTitle("Settings")
            .fileImporter(
                isPresented: $showImporter,
                allowedContentTypes: [.json, .plainText, .data]
            ) { result in
                switch result {
                case .success(let url):
                    let r = InstagramImporter.importFollowing(from: url, context: context)
                    if r.linked == 0 && r.created == 0 && r.skipped == 0 {
                        message = "Nothing imported — is that the following.json from your Instagram export?"
                    } else {
                        message = "Instagram: linked \(r.linked) to contacts, added \(r.created) new, skipped \(r.skipped) already known"
                    }
                case .failure:
                    message = "Import failed — pick the following.json file from your Instagram export"
                }
            }
        }
    }
}
