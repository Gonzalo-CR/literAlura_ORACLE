package com.aluracurso.challenger.literAlura.repository;

import com.aluracurso.challenger.literAlura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    @Query("SELECT l FROM Libro l WHERE LOWER(l.titulo) LIKE LOWER(CONCAT('%', :palabra, '%'))")
    List<Libro> findLibrosCandidatosPorTitulo(String palabra);

    //*********************************

    // Mantener los idiomas únicos
    @Query("SELECT DISTINCT i FROM Libro l JOIN l.idiomas i")
    List<String> findDistinctIdiomas();

    // Libros por idioma
    @Query("""
    SELECT l FROM Libro l 
    JOIN l.idiomas i 
    WHERE LOWER(i) LIKE LOWER(CONCAT('%', :idioma, '%'))
    """)
    List<Libro> findLibrosPorIdioma(String idioma);

    // Verificar si un libro existe con un título específico
    boolean existsByTituloIgnoreCase(String titulo);
}
