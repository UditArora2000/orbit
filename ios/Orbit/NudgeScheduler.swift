import Foundation
import UserNotifications

enum NudgeScheduler {
    static func requestAuthorization() async {
        let center = UNUserNotificationCenter.current()
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
    }

    // Read through UserDefaults with the same keys the SwiftUI views use via @AppStorage.
    static var nudgeEnabled: Bool {
        UserDefaults.standard.object(forKey: "nudgeEnabled") as? Bool ?? true
    }

    static var nudgeHour: Int {
        UserDefaults.standard.object(forKey: "nudgeHour") as? Int ?? 18
    }

    static var nudgeMinute: Int {
        UserDefaults.standard.object(forKey: "nudgeMinute") as? Int ?? 0
    }

    /// iOS won't run app code at notification delivery time, so we plan a week ahead:
    /// the next nudge names the current next-up friend; later days are generic and get
    /// replaced with a named one every time the app runs.
    static func reschedule(people: [Person]) {
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
        guard nudgeEnabled else { return }

        let pick = Rotation.candidates(people).prefix(5).randomElement()
        var fireDate = nextOccurrence(hour: nudgeHour, minute: nudgeMinute)
        for day in 0..<7 {
            let content = UNMutableNotificationContent()
            if day == 0, let person = pick {
                content.title = "Time to catch up with \(person.displayName)"
                content.body = person.lastContactedAt.map { "Last caught up \(relativeTime($0))" }
                    ?? "You two haven't caught up since you started tracking"
            } else {
                content.title = "Time to call a friend"
                content.body = "Open Orbit to see who's next in your rotation"
            }
            content.sound = .default
            let comps = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute], from: fireDate
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
            center.add(UNNotificationRequest(identifier: "nudge-\(day)", content: content, trigger: trigger))
            fireDate = Calendar.current.date(byAdding: .day, value: 1, to: fireDate) ?? fireDate
        }
    }

    static func nextOccurrence(hour: Int, minute: Int, after date: Date = .now) -> Date {
        let calendar = Calendar.current
        var comps = calendar.dateComponents([.year, .month, .day], from: date)
        comps.hour = hour
        comps.minute = minute
        let candidate = calendar.date(from: comps) ?? date
        if candidate > date { return candidate }
        return calendar.date(byAdding: .day, value: 1, to: candidate) ?? candidate
    }

    static func notifyNewContacts(_ count: Int) {
        let content = UNMutableNotificationContent()
        content.title = count == 1 ? "1 new contact to file" : "\(count) new contacts to file"
        content.body = "Open Orbit to add how you know them"
        content.sound = .default
        UNUserNotificationCenter.current().add(
            UNNotificationRequest(identifier: "new-contacts", content: content, trigger: nil)
        )
    }
}
