import SwiftUI

struct PsalmScreen: View {
    var body: some View {
        NavigationView {
            VStack {
                Text("Psalmi")
                    .font(.title2.weight(.semibold))
                    .padding(.top, 32)
                Text("Ovaj tab je trenutno prazan.")
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                Spacer()
            }
            .navigationTitle("Psalmi")
        }
    }
}

struct PsalmScreen_Previews: PreviewProvider {
    static var previews: some View {
        PsalmScreen()
    }
}
