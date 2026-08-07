/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import io.jsonwebtoken.io.IOException;


/**
 *
 * @author sinawatrarith
 */

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Uploads a file to a specific folder in Cloudinary.
     * @throws java.io.IOException
     */
    public Map uploadFile(MultipartFile file, String folderName) throws IOException, java.io.IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folderName,
                "resource_type", "auto" // Detects image, video, or raw file automatically
        ));
    }

    /**
     * Deletes an asset by public_id.
     * @throws java.io.IOException 
     */
    public Map deleteFile(String publicId) throws IOException, java.io.IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}