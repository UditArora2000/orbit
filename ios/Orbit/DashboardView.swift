import SwiftData
import SwiftUI

struct DashboardView: View {
    @Binding var selectedTab: Int

    @Environment(\.modelContext) private var context
    @Environment(\.openURL) private var openURL
    @Query private var people: [Person]
    @Query(sort: \CatchUpLog.timestamp, order: .reverse) private var logs: [CatchUpLog]
    @AppStorage("cycleNumber") private var cycleNumber = 1
    @State private var contactsAuthorized = ContactsSyncer.isAuthorized
    @State private var syncMessage: String?

    private var activeCount: Int { people.filter { $0.status != Person.statusArchived }.count }
    private var untriagedCount: Int { people.filter { $0.status == Person.statusUntriaged }.count }
    private var callable: [Person] { people.filter { $0.status == Person.statusTriaged && $0.canCall } }
    private var hangoutCount: Int { people.filter { $0.status == Person.statusTriaged && $0.canHangout }.count }
    private var tightCount: Int { people.filter { $0.status == Person.statusTriaged && $0.isTight }.count }
    private var cycleDone: Int { callable.filter { $0.contactedThisCycle }.count }
    private var nextUp: Person? { Rotation.candidates(people).first }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Keep your people in orbit")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let msg = syncMessage {
                        Text(msg).font(.caption).foregroundStyle(Color.accentColor)
                    }

                    if !contactsAuthorized {
                        connectCard
                    }

                    filingCard
                    rotationCard

                    HStack(spacing: 12) {
                        StatTile(value: "\(callable.count)", label: "can call")
                        StatTile(value: "\(hangoutCount)", label: "hangout crew")
                    }
                    HStack(spacing: 12) {
                        StatTile(value: "\(tightCount)", label: "tight")
                        StatTile(value: "\(logs.count)", label: "catch-ups logged")
                    }

                    if !logs.isEmpty {
                        Text("Recent catch-ups").font(.headline)
                        ForEach(logs.prefix(8)) { log in
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.circle")
                                    .foregroundStyle(Color.accentColor)
                                VStack(alignment: .leading) {
                                    Text(log.personName)
                                    Text(relativeTime(log.timestamp))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Orbit")
            .task {
                await NudgeScheduler.requestAuthorization()
                if contactsAuthorized { runSync() }
            }
        }
    }

    private var connectCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Connect your contacts").font(.headline)
            Text("Orbit reads your contacts so you can file each person and build your call rotation. Everything stays on this phone.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Button("Allow access") {
                Task {
                    contactsAuthorized = await ContactsSyncer.requestAccess()
                    await NudgeScheduler.requestAuthorization()
                    if contactsAuthorized { runSync() }
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .card()
    }

    private var filingCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("File your people").font(.headline)
            let filed = activeCount - untriagedCount
            LabeledProgress(done: filed, total: activeCount, caption: "\(filed) of \(activeCount) filed")
            if untriagedCount > 0 {
                Button("Continue filing (\(untriagedCount) left)") { selectedTab = 1 }
                    .buttonStyle(.borderedProminent)
            } else if activeCount > 0 {
                Text("All filed — new contacts get queued automatically.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .card()
    }

    private var rotationCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Call rotation · Round \(cycleNumber)").font(.headline)
            if callable.isEmpty {
                Text("No one in your rotation yet. While filing, flip on “Can I call them?” to add friends here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                LabeledProgress(
                    done: cycleDone,
                    total: callable.count,
                    caption: "\(cycleDone) of \(callable.count) called this round"
                )
                if cycleDone >= callable.count {
                    Text("Round \(cycleNumber) complete — you caught up with everyone.")
                    Button("Start round \(cycleNumber + 1)") {
                        for person in people { person.contactedThisCycle = false }
                        cycleNumber += 1
                        try? context.save()
                        NudgeScheduler.reschedule(people: people)
                    }
                    .buttonStyle(.borderedProminent)
                } else if let person = nextUp {
                    HStack(spacing: 12) {
                        AvatarView(person: person)
                        VStack(alignment: .leading) {
                            Text("Next up: \(person.displayName)").font(.headline)
                            Text(person.lastContactedAt.map { "Last caught up \(relativeTime($0))" }
                                ?? "Never caught up (since tracking)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
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
                            NudgeScheduler.reschedule(people: people)
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }
        }
        .card()
    }

    private func runSync() {
        let result = ContactsSyncer.sync(context: context)
        if result.newCount > 0 {
            syncMessage = "Found \(result.newCount) new contact\(result.newCount == 1 ? "" : "s") to file"
        }
        NudgeScheduler.reschedule(people: people)
    }
}
