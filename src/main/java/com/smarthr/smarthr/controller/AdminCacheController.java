/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.controller;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author root
 */
@RestController
@Slf4j
    @RequestMapping("/api/admin/cache")
    @RequiredArgsConstructor
    public class AdminCacheController {
    
        private final CacheManager cacheManager;
    
        // Clear a specific cache by name (e.g., DELETE /api/admin/cache/clear/companies)
        @DeleteMapping("/clear/{cacheName}")
        public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully.");
            }
            return ResponseEntity.notFound().build();
        }

        // Clear ALL caches in Redis (e.g., DELETE /api/admin/cache/clear-all)
        @DeleteMapping("/clear-all")
        public ResponseEntity<String> clearAllCaches() {
            log.info("Clear ALL caches in Redis ");
            for (String name : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
            return ResponseEntity.ok("All Redis caches cleared successfully.");
        }
    }
