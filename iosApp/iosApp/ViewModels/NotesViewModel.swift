import SwiftUI
import shared

class NotesViewModel: ObservableObject {
    @Published var notes: [String] = [] // Simplified - just store strings for now
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let logger = Logger.shared
    
    init() {
        // TODO: Implement Realm-based notes when repository API is available
        logger.info(tag: "NotesViewModel", message: "Notes functionality temporarily disabled", throwable: nil)
    }
    
    func addNote(content: String) {
        // TODO: Implement
        logger.info(tag: "NotesViewModel", message: "Add note not yet implemented", throwable: nil)
    }
    
    func updateNote(id: String, content: String) {
        // TODO: Implement
        logger.info(tag: "NotesViewModel", message: "Update note not yet implemented", throwable: nil)
    }
    
    func deleteNote(id: String) {
        // TODO: Implement
        logger.info(tag: "NotesViewModel", message: "Delete note not yet implemented", throwable: nil)
    }
}
