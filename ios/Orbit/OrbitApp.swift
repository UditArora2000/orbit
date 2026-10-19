import SwiftData
import SwiftUI

@main
struct OrbitApp: App {
    let container: ModelContainer
    @Environment(\.scenePhase) private var scenePhase

    init() {
        do {
            container = try ModelContainer(for: Person.self, CatchUpLog.self)
        } catch {
            fatalError("Failed to create model container: \(error)")
        }
        BackgroundRefresh.register(container: container)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(container)
        .onChange(of: scenePhase) { _, phase in
            if phase == .background {
                BackgroundRefresh.schedule()
            }
        }
    }
}
