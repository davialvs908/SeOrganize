package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.senha = :senha where u.id = :id")
    int atualizarSenha(@Param("id") Long id, @Param("senha") String senha);
}