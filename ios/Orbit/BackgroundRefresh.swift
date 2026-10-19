import BackgroundTasks
import Foundation
import SwiftData

/// Opportunistic background contact sync. iOS decides when (and whether) to run it —
/// there's no guaranteed interval like Android's WorkManager — so new-contact
/// detection also happens every time the app opens.
enum BackgroundRefresh {
    static let taskID = "com.udit.orbit.refresh"

    static func register(container: ModelContainer) {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskID, using: nil) { task in
            guard let refresh = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(refresh, container: container)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: taskID)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 6 * 60 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    private static func handle(_ task: BGAppRefreshTask, container: ModelContainer) {
        schedule()
        let context = ModelContext(container)
        let result = ContactsSyncer.sync(context: context)
        if result.newCount > 0 {
            NudgeScheduler.notifyNewContacts(result.newCount)
        }
        if let people = try? context.fetch(FetchDescriptor<Person>()) {
            NudgeScheduler.reschedule(people: people)
        }
        task.setTaskCompleted(success: true)
    }
}
