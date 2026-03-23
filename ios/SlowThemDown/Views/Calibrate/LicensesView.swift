import SwiftUI

struct OpenSourceLicense: Identifiable {
    let id = UUID()
    let name: String
    let license: String
    let url: String
}

private let licenses: [OpenSourceLicense] = [
    OpenSourceLicense(
        name: "Firebase iOS SDK",
        license: "Apache 2.0",
        url: "https://github.com/firebase/firebase-ios-sdk"
    ),
]

struct LicensesView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            Section {
                Text("Slow Them Down is built with the following open source software.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .listRowBackground(Color.clear)
            }

            Section("Dependencies") {
                ForEach(licenses) { dep in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(dep.name)
                            .font(.body)
                        Text(dep.license)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section("This App") {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Slow Them Down")
                        .font(.body)
                    Text("MIT License")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("Copyright (c) 2026 Duncan Grazier")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Open Source Licenses")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Done") { dismiss() }
            }
        }
    }
}
