package com.aluracurso.challenger.literAlura.repository;

import com.aluracurso.challenger.literAlura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    Optional<Autor> findByNombre(String nombre);

    @Query("""
SELECT a FROM Autor a 
LEFT JOIN FETCH a.bibliografias b 
LEFT JOIN FETCH b.libro 
WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
""")
    List<Autor> findByNombreWithLibros(String nombre);


    Optional<Autor> findFirstByNombreContainingIgnoreCase(String nombre);

    @Query("""
    SELECT a FROM Autor a 
    LEFT JOIN FETCH a.bibliografias b 
    LEFT JOIN FETCH b.libro 
    WHERE a.fechaDeNacimiento <= :ano 
    AND (a.fechaDeFallecimiento IS NULL OR a.fechaDeFallecimiento >= :ano)
""")
    List<Autor> findAutoresVivosEnAno(int ano);

    @Query("SELECT a FROM Autor a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :palabra, '%'))")
    Optional<Autor> findByNombreContainingIgnoreCase(String palabra);

//    @Query("""
//    SELECT a FROM Autor a
//    LEFT JOIN FETCH a.bibliografias b
//    LEFT JOIN FETCH b.libro
//    WHERE a.fechaDeNacimiento <= :ano AND (a.fechaDeFallecimiento IS NULL OR a.fechaDeFallecimiento >= :ano)
//""")
//    List<Autor> findAutoresVivosEnAnoWithLibros(int ano);

}
