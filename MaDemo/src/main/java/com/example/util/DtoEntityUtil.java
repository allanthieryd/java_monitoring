package com.example.util;

import com.example.dto.ProfilDto;
import com.example.entity.Profil;

public class DtoEntityUtil {

	private DtoEntityUtil() {
	}

	public static Profil toEntity(ProfilDto profilDto) {
		Profil profil = new Profil();
		profil.setName(profilDto.getName());
		return profil;
	}

	public static ProfilDto toDto(Profil profil) {
		ProfilDto dto = new ProfilDto();
		dto.setId(profil.getId());
		dto.setName(profil.getName());
		return dto;
    }

}
