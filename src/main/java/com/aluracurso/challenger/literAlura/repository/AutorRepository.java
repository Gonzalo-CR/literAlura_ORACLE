package com.aluracurso.challenger.literAlura.repository;

import com.aluracurso.challenger.literAlura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

//    // Busca el primer autor que coincida exactamente con el nombre proporcionado
//    @Query("""
//        SELECT a FROM Autor a
//        WHERE LOWER(a.nombre) = LOWER(:nombre)
//    """)
//    Optional<Autor> findByNombreExacto(@Param("nombre") String nombre);
//
//    // Busca el primer autor que contenga la palabra clave (palabras clave parciales)
//    @Query("""
//        SELECT a FROM Autor a
//        WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :palabraClave, '%'))
//    """)
//    Optional<Autor> findFirstByNombrePalabrasClave(@Param("palabraClave") String palabraClave);
//
//
//
//    @Query("SELECT a FROM Autor a WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :palabra, '%'))")
//    Optional<Autor> findByNombreContainingIgnoreCase(String palabra);


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

//    // Consulta para obtener un autor junto con sus libros
//    @Query("""
//        SELECT a FROM Autor a
//        LEFT JOIN FETCH a.bibliografias b
//        LEFT JOIN FETCH b.libro
//        WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
//    """)
//    List<Autor> findByNombreWithLibros(@Param("nombre") String nombre);


}
