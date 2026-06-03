import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var favoritesViewModel = FavoritesViewModel()
    @State private var selectedTab = 0

    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(hex: "0F1419"),
                    Color(hex: "1A1E23")
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            TabView(selection: $selectedTab) {
                // Home Tab - Videos List
                NavigationContainer {
                    HomeScreen()
                        .environmentObject(favoritesViewModel)
                }
                .tabItem {
                    Label(l10n("home"), systemImage: "house.fill")
                }
                .tag(0)

                // Search Tab
                NavigationContainer {
                    SearchScreen()
                        .environmentObject(favoritesViewModel)
                }
                .tabItem {
                    Label(l10n("search"), systemImage: "magnifyingglass")
                }
                .tag(1)

                // Rate Video Tab
                NavigationContainer {
                    RatingView(viewModel: RatingViewModel())
                }
                .tabItem {
                    Label(l10n("rate"), systemImage: "star.fill")
                }
                .tag(2)

                // Rated Videos Tab
                NavigationContainer {
                    RatedVideosScreen()
                        .environmentObject(favoritesViewModel)
                }
                .tabItem {
                    Label(l10n("my_ratings_title"), systemImage: "list.star")
                }
                .tag(3)

                // Notes Tab
                NavigationContainer {
                    NotesScreen()
                }
                .tabItem {
                    Label(l10n("tab_name_notes"), systemImage: "note.text")
                }
                .tag(4)

                // Settings Tab
                NavigationContainer {
                    SettingsScreen()
                }
                .tabItem {
                    Label(l10n("settings_title"), systemImage: "gear")
                }
                .tag(5)
            }
            .accentColor(Color(hex: "00D4FF")) // Electric cyan accent
            .modifier(EnhancedTabBar())

            #if DEBUG
            VStack {
                Text(l10n("ui_ok_debug"))
                    .font(.caption.bold())
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.red.opacity(0.9))
                    .foregroundColor(.white)
                    .cornerRadius(8)
                    .padding(.top, 8)
                Spacer()
            }
            .allowsHitTesting(false)
            #endif
        }
        .preferredColorScheme(.dark)
    }
}

private struct NavigationContainer<Content: View>: View {
    @ViewBuilder let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack { content }
        } else {
            NavigationView { content }
        }
    }
}

// Enhanced Tab Bar Style
struct EnhancedTabBar: ViewModifier {
    func body(content: Content) -> some View {
        content
            .onAppear {
                let appearance = UITabBarAppearance()
                appearance.configureWithTransparentBackground()
                appearance.backgroundColor = UIColor(Color(hex: "1A1E23").opacity(0.8))

                // Configure colors
                appearance.stackedLayoutAppearance.selected.iconColor = UIColor(Color(hex: "00D4FF"))
                appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
                    .foregroundColor: UIColor(Color(hex: "00D4FF"))
                ]
                appearance.stackedLayoutAppearance.normal.iconColor = UIColor(Color(hex: "8B9198"))
                appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
                    .foregroundColor: UIColor(Color(hex: "8B9198"))
                ]

                UITabBar.appearance().standardAppearance = appearance
                UITabBar.appearance().scrollEdgeAppearance = appearance
            }
    }
}

// Color extension for hex colors
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
