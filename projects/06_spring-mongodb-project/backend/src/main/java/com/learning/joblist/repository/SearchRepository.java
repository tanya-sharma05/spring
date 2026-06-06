package com.learning.joblist.repository;

import com.learning.joblist.model.Post;

import java.util.List;

public interface SearchRepository {

    List<Post>  findByText(String text);
}
