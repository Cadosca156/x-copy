package com.net.cardo.notebookbackend.repository;

import com.net.cardo.notebookbackend.entity.Note;
import com.net.cardo.notebookbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface NoteRepository extends JpaRepository<Note,Long> {
    List<Note> findByUser(User user);
}
