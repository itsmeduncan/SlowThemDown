import FirebaseCore
import SwiftData
import SwiftUI

@main
struct SlowThemDownApp: App {
    let container: ModelContainer

    init() {
        #if !DEBUG
        FirebaseApp.configure()
        #endif

        let schema = Schema(versionedSchema: SpeedEntrySchemaV2.self)
        let config = ModelConfiguration(schema: schema)

        do {
            container = try ModelContainer(
                for: schema,
                migrationPlan: SpeedEntryMigrationPlan.self,
                configurations: config
            )
        } catch {
            #if DEBUG
            // Pre-release only: delete unversioned store so dev simulators recover.
            // Remove this fallback before shipping v1 — production upgrades must
            // go through the migration plan, never silently drop user data.
            Self.deleteSwiftDataStore(at: config.url)
            do {
                container = try ModelContainer(
                    for: schema,
                    migrationPlan: SpeedEntryMigrationPlan.self,
                    configurations: config
                )
            } catch {
                fatalError("Failed to create model container after store reset: \(error)")
            }
            #else
            fatalError("SwiftData migration failed: \(error)")
            #endif
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

    private static func deleteSwiftDataStore(at url: URL) {
        let fm = FileManager.default
        // Remove the store file and its SQLite companions
        for ext in ["", "-wal", "-shm"] {
            let fileURL = ext.isEmpty ? url : URL(fileURLWithPath: url.path + ext)
            try? fm.removeItem(at: fileURL)
        }
        // Also try the default SwiftData location in Application Support
        if let appSupport = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            let defaultStore = appSupport.appendingPathComponent("default.store")
            for ext in ["", "-wal", "-shm"] {
                let fileURL = ext.isEmpty ? defaultStore : URL(fileURLWithPath: defaultStore.path + ext)
                try? fm.removeItem(at: fileURL)
            }
        }
    }

    private func applyAppearance() {
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithDefaultBackground()
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
    }
}
