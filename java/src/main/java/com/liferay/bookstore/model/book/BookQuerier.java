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

package com.liferay.bookstore.model.book;

import com.liferay.bookstore.model.author.AuthorQuerier;
import com.liferay.bookstore.service.ReadOnlyContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface BookQuerier {

	public long id();
	public String isbn();
	public String title();
	public ReadOnlyContext<AuthorQuerier> fromAuthor();

	public static Optional<BookQuerier> fromResultSet(ResultSet resultSet) {
		try {
			if (!resultSet.next()) {
				return Optional.empty();
			}

			final long id = resultSet.getLong("id");
			final String isbn = resultSet.getString("isbn");
			final String title = resultSet.getString("title");

			return Optional.of(new BookQuerier() {
				@Override
				public long id() {
					return id;
				}

				@Override
				public String isbn() {
					return isbn;
				}

				@Override
				public String title() {
					return title;
				}

				@Override
				public ReadOnlyContext<AuthorQuerier> fromAuthor() {
					return null;
				}
			});
		}
		catch (SQLException e) {
			e.printStackTrace();

			return Optional.empty();
		}
	}
}
