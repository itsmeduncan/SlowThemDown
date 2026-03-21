import Charts
import SwiftData
import SwiftUI

struct ReportView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \SpeedEntry.timestamp, order: .reverse) private var entries: [SpeedEntry]
    @State private var vm = ReportViewModel()
    @State private var showShareSheet = false
    @State private var shareURL: URL?
    @State private var isExporting = false
    @State private var showingDemoData = SeedData.isSeeded

    var body: some View {
        NavigationStack {
            ScrollView {
                if showingDemoData {
                    DemoBanner {
                        SeedData.clearDemoData(context: modelContext)
                        showingDemoData = false
                    }
                    .padding(.horizontal)
                }

                if entries.isEmpty {
                    ContentUnavailableView(
                        "No Data",
                        systemImage: "chart.bar",
                        description: Text("Capture some speed measurements to see reports.")
                    )
                } else {
                    VStack(spacing: 20) {
                        streetFilterSection
                        v85Section
                        metricsGrid
                        histogramChart
                        hourlyChart
                        scatterChart
                        if vm.selectedStreet == nil && vm.streetGroups.count > 1 {
                            streetBreakdownSection
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Reports")
            .toolbar {
                if !entries.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Menu {
                            Button {
                                exportCSV()
                            } label: {
                                Label("Export CSV", systemImage: "tablecells")
                            }
                            Button {
                                exportPDF()
                            } label: {
                                Label("Export PDF", systemImage: "doc.richtext")
                            }
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                        }
                    }
                }
            }
            .overlay {
                if isExporting {
                    ZStack {
                        Color.black.opacity(0.4)
                            .ignoresSafeArea()
                        VStack(spacing: 12) {
                            ProgressView()
                                .controlSize(.large)
                            Text("Generating report…")
                                .font(.subheadline)
                                .foregroundStyle(.white)
                        }
                        .padding(24)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let url = shareURL {
                    ShareSheet(items: [url])
                }
            }
            .onChange(of: entries.count) {
                vm.update(with: entries)
            }
            .onAppear {
                vm.update(with: entries)
            }
        }
    }

    // MARK: - Street Filter

    private var streetFilterSection: some View {
        Group {
            if vm.availableStreets.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        streetFilterChip(label: "All Streets", street: nil)
                        ForEach(vm.availableStreets, id: \.self) { street in
                            streetFilterChip(label: street, street: street)
                        }
                    }
                }
            }
        }
    }

    private func streetFilterChip(label: String, street: String?) -> some View {
        let isSelected = vm.selectedStreet == street
        return Button {
            vm.selectStreet(street)
        } label: {
            Text(label)
                .font(.subheadline)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.accentColor : Color(.systemGray5))
                .foregroundStyle(isSelected ? .black : .primary)
                .clipShape(Capsule())
        }
    }

    // MARK: - Street Breakdown

    private var streetBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("By Street")
                .font(.headline)

            ForEach(vm.streetGroups) { group in
                Button {
                    vm.selectStreet(group.name)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(group.name)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text("\(group.count) entries · Avg \(String(format: "%.1f", group.meanSpeed)) MPH")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "%.0f%%", group.overLimitPercent))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(group.overLimitPercent > 50 ? .red : .green)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .background(Color(.systemGray6).opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - V85 Section

    private var v85Section: some View {
        Group {
            if let stats = vm.stats {
                let mostCommonLimit = mostCommonSpeedLimit()
                V85CardView(v85: stats.v85, speedLimit: mostCommonLimit)
            }
        }
    }

    // MARK: - Metrics Grid

    private var metricsGrid: some View {
        Group {
            if let stats = vm.stats {
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                ], spacing: 12) {
                    MetricCard(title: "Mean", value: String(format: "%.1f", stats.mean), unit: "MPH")
                    MetricCard(title: "Median", value: String(format: "%.1f", stats.median), unit: "MPH")
                    MetricCard(title: "Total", value: "\(stats.count)", unit: "entries")
                    MetricCard(
                        title: "Over Limit",
                        value: String(format: "%.0f%%", stats.overLimitPercent),
                        unit: "\(stats.overLimitCount) vehicles",
                        color: stats.overLimitPercent > 50 ? .red : .green
                    )
                }
            }
        }
    }

    // MARK: - Charts

    private var histogramChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speed Distribution")
                .font(.headline)
            Chart(vm.histogram) { bucket in
                BarMark(
                    x: .value("Speed Range", bucket.range),
                    y: .value("Count", bucket.count)
                )
                .foregroundStyle(Color.accentColor.gradient)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var hourlyChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Average Speed by Hour")
                .font(.headline)
            Chart(vm.hourlyAverages) { item in
                LineMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", item.averageSpeed)
                )
                .foregroundStyle(Color.orange)
                .interpolationMethod(.catmullRom)

                PointMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", item.averageSpeed)
                )
                .foregroundStyle(Color.orange)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var scatterChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speeds Over Time")
                .font(.headline)
            Chart(vm.dailyEntries) { item in
                PointMark(
                    x: .value("Date", item.date),
                    y: .value("Speed", item.speed)
                )
                .foregroundStyle(Color.blue)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Helpers

    private func mostCommonSpeedLimit() -> Int {
        let active = vm.filteredEntries
        let limits = active.map(\.speedLimit)
        let counts = Dictionary(grouping: limits, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key ?? RoadStandards.defaultSpeedLimit
    }

    private func exportCSV() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        Task.detached {
            let url = ReportExporter.csvFileURL(entries: capturedEntries)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareURL = url
                    showShareSheet = true
                }
            }
        }
    }

    private func exportPDF() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        let stats = vm.stats
        Task.detached {
            let url = ReportExporter.pdfFileURL(entries: capturedEntries, stats: stats)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareURL = url
                    showShareSheet = true
                }
            }
        }
    }
}

// MARK: - ShareSheet

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
