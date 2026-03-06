import SwiftData
import SwiftUI

@main
struct SlowThemDownApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
                .onAppear {
                    applyAppearance()
                }
        }
        .modelContainer(for: SpeedEntry.self) { result in
            #if DEBUG
            if case .success(let container) = result {
                SeedData.seedIfEmpty(context: container.mainContext)
            }
            #endif
        }
    }

    private func applyAppearance() {
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithDefaultBackground()
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
    }
}
