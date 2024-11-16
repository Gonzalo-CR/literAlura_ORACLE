package com.aluracurso.challenger.literAlura.repository;

import com.aluracurso.challenger.literAlura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    Optional<Autor> findByNombre(String nombre);

    @Query("SELECT a FROM Autor a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :palabra, '%'))")
    List<Autor> findByNombreCandidato(@Param("palabra") String palabra);

    // Encuentra autores vivos en un año específico
    @Query("""
        SELECT a FROM Autor a 
        WHERE a.fechaDeNacimiento <= :ano 
        AND (a.fechaDeFallecimiento IS NULL OR a.fechaDeFallecimiento >= :ano)
    """)
    List<Autor> findAutoresVivosEnAno(@Param("ano") int ano);
}
