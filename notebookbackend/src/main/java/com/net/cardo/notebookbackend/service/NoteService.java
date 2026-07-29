package com.net.cardo.notebookbackend.service;


import com.net.cardo.notebookbackend.entity.User;
import com.net.cardo.notebookbackend.repository.NoteRepository;
import com.net.cardo.notebookbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.net.cardo.notebookbackend.entity.Note;


import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoteService {

private final NoteRepository noteRepository;
private final UserRepository userRepository;


public Note postNote(Note note){
    Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

    String email = authentication.getName();

User user = userRepository.findByEmail(email)
        .orElseThrow(()->new EntityNotFoundException("User not found"));


    note.setUser(user);

return noteRepository.save(note);

}
public List<Note> getAllNotes(){
    return noteRepository.findAll();
}

public void deleteNote(Long id){
    if (!noteRepository.existsById(id)){
        throw new EntityNotFoundException("Note Not Found");
    }
    noteRepository.deleteById(id);
}
public Note getNoteById(Long id){
    return noteRepository.findById(id).orElse(null);
}


public Note completeNote(Long id,Note note){
    Optional<Note> optionalNote = noteRepository.findById(id);
     if (optionalNote.isPresent()){
    Note existingNote = optionalNote.get();

    existingNote.setCompleted(note.isCompleted());
    return noteRepository.save(existingNote);
     }
     return null;


}
    public Note updateNote(Long id, Note note){

        Note existingNote = noteRepository.findById(id).orElse(null);

        if(existingNote == null){
            return null;
        }



        existingNote.setText(note.getText());
        existingNote.setDate(note.getDate());
        Note saved = noteRepository.save(existingNote);


        return saved;
    }




}
