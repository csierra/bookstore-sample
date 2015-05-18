/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package org.funbizmodel.bookstore.service;

import org.funbizmodel.bookstore.Command;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Carlos Sierra Andrés
 */
public interface SqlCommand<Q> extends Command<SqlCommandContext<Q>> {

	@Override
	default SqlCommand<Q> andThen(
		Consumer<? super SqlCommandContext<Q>> after) {
		Objects.requireNonNull(after);

		return (SqlCommandContext<Q> t) -> { accept(t); after.accept(t); };
	}
}
