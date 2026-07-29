package com.net.cardo.notebookbackend.controller;

import com.net.cardo.notebookbackend.repository.NoteRepository;
import com.net.cardo.notebookbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.net.cardo.notebookbackend.entity.User;

import com.net.cardo.notebookbackend.entity.Note;

import java.util.List;
import com.net.cardo.notebookbackend.service.NoteService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NotesController {
    private final NoteService noteService;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;



    @PostMapping
    public Note postNote(@RequestBody Note note) {return noteService.postNote(note);
    }
    @GetMapping
public List<Note> getAllNotes(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return noteRepository.findByUser(user);}

    @DeleteMapping("/{id}")
public ResponseEntity<?> deleteNote(@PathVariable Long id){
        try{
            noteService.deleteNote(id);
            return new ResponseEntity<>("Note Deleted", HttpStatus.OK);
        }
catch(EntityNotFoundException e){
            return new ResponseEntity<>("Note Not Found", HttpStatus.NOT_FOUND);
}
    }

    @PatchMapping("/{id}")
public ResponseEntity<?> completeNote(@PathVariable Long id, @RequestBody Note note) {

        Note completedNote = noteService.completeNote(id, note);
        if (completedNote == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(completedNote);
    }

    @PatchMapping("/update/{id}")
        public ResponseEntity<?>  updateNote(@PathVariable Long id, @RequestBody Note note){

    Note updateNote = noteService.updateNote(id, note);
        if(updateNote == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(updateNote);
        }

}
