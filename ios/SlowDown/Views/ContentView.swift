import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            CaptureView()
                .tabItem {
                    Label("Capture", systemImage: "video.fill")
                }

            CalibrateView()
                .tabItem {
                    Label("Calibrate", systemImage: "ruler")
                }

            LogView()
                .tabItem {
                    Label("Log", systemImage: "list.bullet")
                }

            ReportView()
                .tabItem {
                    Label("Reports", systemImage: "chart.bar.fill")
                }
        }
    }
}
