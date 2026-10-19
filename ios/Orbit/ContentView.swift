import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView(selectedTab: $selectedTab)
                .tabItem { Label("Home", systemImage: "house") }
                .tag(0)
            TriageView()
                .tabItem { Label("File", systemImage: "person.text.rectangle") }
                .tag(1)
            FriendsView()
                .tabItem { Label("Friends", systemImage: "person.2") }
                .tag(2)
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag(3)
        }
    }
}
