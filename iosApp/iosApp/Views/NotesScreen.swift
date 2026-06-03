import SwiftUI
import shared

struct NotesScreen: View {
    @StateObject private var viewModel = NotesViewModel()
    @State private var showAddNote = false
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.notes.isEmpty {
                    emptyState
                } else {
                    notesList
                }
            }
            .navigationTitle(l10n("tab_name_notes"))
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showAddNote = true }) {
                        Image(systemName: "plus")
                    }
                    .disabled(true) // Disabled until functionality is implemented
                }
            }
            .alert(l10n("error"), isPresented: .constant(viewModel.errorMessage != nil)) {
                Button(l10n("ok")) {
                    viewModel.errorMessage = nil
                }
            } message: {
                if let error = viewModel.errorMessage {
                    Text(error)
                }
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "note.text")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            
            Text(l10n("no_notes_yet"))
                .font(.title2)
                .fontWeight(.medium)
            
            Text(l10n("notes_first_hint"))
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
    
    private var notesList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                Text(l10n("notes_coming_soon"))
                    .foregroundColor(.secondary)
                    .padding()
            }
            .padding()
        }
    }
}

// Simplified note card for future use
struct NoteCard: View {
    let note: String
    
    var body: some View {
        Text(note)
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
    }
}
