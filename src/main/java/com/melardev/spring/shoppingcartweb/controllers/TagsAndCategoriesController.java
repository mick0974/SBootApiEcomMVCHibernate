package com.melardev.spring.shoppingcartweb.controllers;

import com.melardev.spring.shoppingcartweb.dtos.response.base.AppResponse;
import com.melardev.spring.shoppingcartweb.dtos.response.base.ErrorResponse;
import com.melardev.spring.shoppingcartweb.dtos.response.categories.CategoriesListResponse;
import com.melardev.spring.shoppingcartweb.dtos.response.categories.SingleCategoryDto;
import com.melardev.spring.shoppingcartweb.dtos.response.tags.SingleTagDto;
import com.melardev.spring.shoppingcartweb.dtos.response.tags.TagsListResponse;
import com.melardev.spring.shoppingcartweb.enums.CrudOperation;
import com.melardev.spring.shoppingcartweb.errors.exceptions.PermissionDeniedException;
import com.melardev.spring.shoppingcartweb.models.Category;
import com.melardev.spring.shoppingcartweb.models.Tag;
import com.melardev.spring.shoppingcartweb.models.User;
import com.melardev.spring.shoppingcartweb.services.CategoryService;
import com.melardev.spring.shoppingcartweb.services.StorageService;
import com.melardev.spring.shoppingcartweb.services.TagService;
import com.melardev.spring.shoppingcartweb.services.auth.AuthorizationService;
import com.melardev.spring.shoppingcartweb.services.auth.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
public class TagsAndCategoriesController {

    @Autowired
    TagService tagService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("tags")
    public TagsListResponse getTags(@RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "page_size", defaultValue = "500") int pageSize,
                                    HttpServletRequest request) {

        List<Tag> tags = tagService.getAllTags();
        return TagsListResponse.build(tags, request.getRequestURI());
    }


    @GetMapping("categories")
    public CategoriesListResponse getCategories(@RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "page_size", defaultValue = "500") int pageSize,
                                                HttpServletRequest request) {
        // Page<Category> categories = categoryService.findLatest(page, pageSize);
        List<Category> categories = categoryService.getAllSummary();
        return CategoriesListResponse.build(categories, request.getRequestURI());
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("tags")
    //public ResponseEntity<AppResponse> createTag(HttpServletRequest request, @RequestParam("images[]") MultipartFile[] uploadingFiles) {
    public ResponseEntity<AppResponse> createTag(HttpServletRequest request, @RequestParam(name="images[]", required = false) MultipartFile[] uploadingFiles) {

        try {
            User user = usersService.getCurrentLoggedInUser();
            if (!this.authorizationCheckOnTags(CrudOperation.CREATE, user))
                throw new PermissionDeniedException("You are not allowed to create products");

            String name = request.getParameter("name");
            String description = request.getParameter("description");

            //List<File> files = storageService.upload(uploadingFiles, "/images/tags");
            List<File> files = new LinkedList<>();

            Tag tag = tagService.create(name, description, files);
            return new ResponseEntity<>(SingleTagDto.build(tag, true, true), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        /*
        catch (IOException e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

         */
    }

    static String extractPostRequestBody(HttpServletRequest request) throws IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            Scanner s = new Scanner(request.getInputStream(), "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
        return "";
    }

    @PreAuthorize("hasRole('ADMIN')") // Remember, if you do not set the prefix, it will add ROLE_ prefix for you
    @PostMapping("categories")
    //public ResponseEntity<AppResponse> createCategory(HttpServletRequest request, @RequestParam("images[]") MultipartFile[] uploadingFiles) {
    public ResponseEntity<AppResponse> createCategory(HttpServletRequest request,
                                                      @RequestParam(name="images[]", required=false) MultipartFile[] uploadingFiles) {

        try {
            User user = usersService.getCurrentLoggedInUser();
            if (!this.authorizationCheckOnCategories(CrudOperation.CREATE, user))
                throw new PermissionDeniedException("You are not allowed to create products");

            String name = request.getParameter("name");
            String description = request.getParameter("description");

            System.out.println("Request: " + request.getParameterMap());
            for (String key: request.getParameterMap().keySet())
                System.out.println("key " + key + " -> " + "value : " + Arrays.toString(request.getParameterMap().get(key)));

            System.out.println("Request body: " + extractPostRequestBody(request));

            System.out.println("Request request url: " + request.getRequestURL());
            System.out.println("Request method: " + request.getMethod());
            System.out.println("Name posted: " + name);
            System.out.println("Description posted: " + description);

            // List<File> files = storageService.upload(uploadingFiles, "/images/categories");
            List<File> files = new LinkedList<>();

            Category category = categoryService.create(name, description, files);
            return new ResponseEntity<>(SingleCategoryDto.build(category, true, true), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        /*
        catch (IOException e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

         */
    }

    private boolean authorizationCheckOnCategories(CrudOperation crudOperation, User user) {
        return authorizationCheckCategories(crudOperation, user, null);
    }

    private boolean authorizationCheckOnTags(CrudOperation operation, User user) {
        return this.authorizationCheckOnTags(operation, user, null);
    }

    private boolean authorizationCheckOnTags(CrudOperation operation, User user, Tag tag) {
        switch (operation) {
            case CREATE:
                return authorizationService.canCreateTags(user);
            case UPDATE:
                return authorizationService.canUpdateTags(user, tag);
            case DELETE:
                return authorizationService.canDeleteTags(user, tag);
            case READ:
                return authorizationService.canReadTags(user);
            default:
                return false;
        }
    }

    private boolean authorizationCheckCategories(CrudOperation operation, User user, Category category) {
        switch (operation) {
            case CREATE:
                return authorizationService.canCreateCategories(user);
            case UPDATE:
                return authorizationService.canUpdateCategories(user, category);
            case DELETE:
                return authorizationService.canDeleteCategories(user);
            case READ:
                return authorizationService.canReadCategories(user);
            default:
                return false;
        }
    }

}
