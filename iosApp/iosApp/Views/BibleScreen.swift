import SwiftUI

struct BibleScreen: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 12) {
                Text("Biblija (uskoro)")
                    .font(.title2.weight(.semibold))
                Text("Offline sadržaj nije uključen na iOS verziji. Dodajte datoteke kada budu spremne.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                Spacer()
            }
            .padding(.top, 32)
            .navigationTitle("Biblija")
        }
    }
}

struct BibleScreen_Previews: PreviewProvider {
    static var previews: some View {
        BibleScreen()
    }
}
