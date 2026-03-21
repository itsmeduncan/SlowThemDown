import FirebaseCore
import SwiftData
import SwiftUI

@main
struct SlowThemDownApp: App {
    let container: ModelContainer

    init() {
        FirebaseApp.configure()

        let schema = Schema(versionedSchema: SpeedEntrySchemaV2.self)
        let config = ModelConfiguration(schema: schema)

        do {
            container = try ModelContainer(
                for: schema,
                migrationPlan: SpeedEntryMigrationPlan.self,
                configurations: config
            )
        } catch {
            // Migration from unversioned schema failed — delete old store and start fresh
            // This only affects development builds; no production users exist yet
            let storeURL = config.url
            try? FileManager.default.removeItem(at: storeURL)
            // Also remove WAL and SHM files
            let walURL = storeURL.appendingPathExtension("wal")
            let shmURL = storeURL.appendingPathExtension("shm")
            try? FileManager.default.removeItem(at: walURL)
            try? FileManager.default.removeItem(at: shmURL)

            do {
                container = try ModelContainer(
                    for: schema,
                    migrationPlan: SpeedEntryMigrationPlan.self,
                    configurations: config
                )
            } catch {
                fatalError("Failed to create model container after store reset: \(error)")
            }
        }

        #if DEBUG
        SeedData.seedIfEmpty(context: container.mainContext)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
                .onAppear {
                    applyAppearance()
                }
        }
        .modelContainer(container)
    }

    private func applyAppearance() {
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithDefaultBackground()
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
    }
}
