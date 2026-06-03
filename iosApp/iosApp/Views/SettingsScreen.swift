import SwiftUI
import shared

struct SettingsScreen: View {
    @Environment(\.openURL) private var openURL
    @AppStorage("loggingEnabled") private var loggingEnabled = true
    @AppStorage("notificationsEnabled") private var notificationsEnabled = false
    @AppStorage("autoRefresh") private var autoRefresh = true
    @State private var showClearDataAlert = false
    @State private var showAboutSheet = false
    
    private let stripeDonationUrl = URL(string: "https://donate.stripe.com/3cI7sK8vcdBC6871e17ss00")
    private let privacyPolicyUrl = URL(string: "https://tmbv-hms.com/privacy-policy.php")

    private let logger = Logger.shared
    
    var body: some View {
        NavigationView {
            Form {
                // Logging Section
                Section {
                    Toggle(l10n("enable_logging"), isOn: $loggingEnabled)
                        .onChange(of: loggingEnabled) { newValue in
                            logger.isEnabled = newValue
                            logger.info(tag: l10n("settings_title"), message: "Logging \(newValue ? "enabled" : "disabled")", throwable: nil)
                        }
                } header: {
                    Text(l10n("debug"))
                } footer: {
                    Text(l10n("logging_help"))
                }
                
                // App Preferences
                Section {
                    Toggle(l10n("notifications"), isOn: $notificationsEnabled)
                    Toggle(l10n("auto_refresh"), isOn: $autoRefresh)
                } header: {
                    Text(l10n("preferences"))
                } footer: {
                    Text(l10n("preferences_subtitle"))
                }
                
                // Data Management
                Section {
                    Button(l10n("clear_all_data"), role: .destructive) {
                        showClearDataAlert = true
                    }
                    .disabled(true) // TODO: Implement clear data functionality
                } header: {
                    Text(l10n("section_data_management"))
                } footer: {
                    Text(l10n("data_clearing_disabled"))
                }
                
                // About
                Section {
                    Button("Doniraj") {
                        if let stripeDonationUrl {
                            openURL(stripeDonationUrl)
                        }
                    }

                    Button("Politika privatnosti") {
                        if let privacyPolicyUrl {
                            openURL(privacyPolicyUrl)
                        }
                    }

                    Button(l10n("about")) {
                        showAboutSheet = true
                    }
                    
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text(l10n("about"))
                }
            }
            .navigationTitle(l10n("settings_title"))
            .alert(l10n("clear_all_data_confirm_title"), isPresented: $showClearDataAlert) {
                Button(l10n("cancel"), role: .cancel) {}
                Button(l10n("clear_button"), role: .destructive) {
                    clearAllData()
                }
            } message: {
                Text(l10n("clear_all_data_confirm_message"))
            }
            .sheet(isPresented: $showAboutSheet) {
                AboutSheet()
            }
        }
    }
    
    private func clearAllData() {
        // TODO: Implement data clearing through repository APIs
        logger.info(tag: l10n("settings_title"), message: l10n("clear_data_not_implemented"), throwable: nil)
    }
}

struct AboutSheet: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // App Icon
                    Image(systemName: "play.rectangle.fill")
                        .font(.system(size: 80))
                        .foregroundColor(.red)
                        .padding(.top, 32)
                    
                    // App Name
                    Text(l10n("app_title"))
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text("Version 1.0.0")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    // Description
                    VStack(spacing: 12) {
                        Text(l10n("rate_youtube_videos"))
                            .font(.body)
                            .multilineTextAlignment(.center)
                        
                        HStack(spacing: 20) {
                            VStack {
                                Image(systemName: "heart.fill")
                                    .foregroundColor(.red)
                                Text(l10n("love"))
                                    .font(.caption)
                            }
                            
                            VStack {
                                Image(systemName: "heart.fill")
                                    .foregroundColor(.blue)
                                Text(l10n("faith"))
                                    .font(.caption)
                            }
                            
                            VStack {
                                Image(systemName: "heart.fill")
                                    .foregroundColor(.green)
                                Text(l10n("hope"))
                                    .font(.caption)
                            }
                        }
                        .font(.title2)
                    }
                    .padding(.vertical)
                    
                    // Features
                    VStack(alignment: .leading, spacing: 16) {
                        FeatureRow(icon: "star.fill", title: l10n("rate_videos"), description: l10n("score_videos_subtitle"))
                        FeatureRow(icon: "heart.fill", title: l10n("save_favorites"), description: l10n("favorites_subtitle"))
                        FeatureRow(icon: "note.text", title: l10n("take_notes"), description: l10n("notes_subtitle"))
                        FeatureRow(icon: "arrow.clockwise", title: l10n("sync_data"), description: l10n("sync_ratings_hint"))
                    }
                    .padding()
                    
                    // Tech Stack
                    VStack(spacing: 8) {
                        Text(l10n("built_with"))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Text(l10n("kotlin_multiplatform"))
                            .font(.caption)
                            .fontWeight(.medium)
                    }
                    .padding(.bottom, 32)
                }
                .padding()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(l10n("done")) {
                        dismiss()
                    }
                }
            }
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.accentColor)
                .font(.title3)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}
