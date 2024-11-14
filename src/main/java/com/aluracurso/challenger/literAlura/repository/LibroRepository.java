package com.aluracurso.challenger.literAlura.repository;

import com.aluracurso.challenger.literAlura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    // Método para buscar libros por título (ignorando mayúsculas y minúsculas)
    Optional<Libro> findFirstByTituloContainingIgnoreCase(String palabra);

    @Query("SELECT DISTINCT i FROM Libro l JOIN l.idiomas i")
    List<String> findDistinctIdiomas();

    @Query("""
    SELECT l FROM Libro l 
    JOIN l.idiomas i 
    WHERE LOWER(i) LIKE LOWER(CONCAT('%', :idioma, '%'))
    """)
    List<Libro> findLibrosPorIdioma(String idioma);

    // Método adicional para verificar si un libro con un título específico ya existe (ignorando mayúsculas y minúsculas)
    boolean existsByTituloIgnoreCase(String titulo);
}
