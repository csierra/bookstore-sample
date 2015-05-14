/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.funbizmodel.bookstore;

import org.funbizmodel.bookstore.model.author.AuthorQuerier;
import org.funbizmodel.bookstore.model.author.AuthorService;
import org.funbizmodel.bookstore.model.book.BookService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * @author Carlos Sierra Andrés
 */
public class Main {

	public static void main(String[] args)
		throws ClassNotFoundException, SQLException {

		Class.forName("org.h2.Driver");

		try (Connection conn = DriverManager.getConnection(
			"jdbc:h2:./bookstore", "sa", "")) {

			AuthorService author = new AuthorService(conn);

			BookService books = new BookService(conn);

			author.setBookService(books);
			books.setAuthorService(author);

			books
				.fromTitles("A new book", "Uno").map(
				bc -> bc
					.andMap(
						bq -> bq.author().andMap(AuthorQuerier::name)))
				.forEach(System.out::println);


		}
	}

}
