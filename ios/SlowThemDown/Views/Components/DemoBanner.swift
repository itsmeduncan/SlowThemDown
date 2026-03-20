import SwiftUI

#if DEBUG
struct DemoBanner: View {
    let onClear: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "flask")
                .foregroundStyle(.orange)
            Text("Showing demo data")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer()
            Button("Clear", role: .destructive, action: onClear)
                .font(.subheadline)
        }
        .listRowBackground(Color.orange.opacity(0.1))
    }
}
#endif
