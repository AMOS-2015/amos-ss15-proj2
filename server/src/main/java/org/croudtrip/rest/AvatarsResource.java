/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.account.Avatar;
import org.croudtrip.account.AvatarManager;
import org.croudtrip.account.UserManager;
import org.croudtrip.api.account.UserDescription;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing users.
 */
@Path("/avatars")
public class AvatarsResource {

    private final UserManager userManager;
    private final AvatarManager avatarManager;

    @Inject
    AvatarsResource(UserManager userManager, AvatarManager avatarManager) {
        this.userManager = userManager;
        this.avatarManager = avatarManager;
    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public User uploadFile(
            @Auth User user,
            @Context UriInfo uriInfo,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

        // check image type
        String mediaType = "image/";
        String fileName = fileDetail.getFileName().toLowerCase();


        if (fileName.endsWith(".png")) mediaType = mediaType + "png";
        else if (fileName.endsWith(".jpg")) mediaType = mediaType + "jpg";
        else if (fileName.endsWith(".jpeg")) mediaType = mediaType + "jpeg";
        else if (fileName.endsWith(".gif")) mediaType = mediaType + "gif";
        else throw RestUtils.createJsonFormattedException("only png, jpg and gif supported", 400);

        // check file size
        int maxSize = 1024 * 5;
        if (fileDetail.getSize() >  maxSize) throw RestUtils.createJsonFormattedException("image must be smaller than " + maxSize + " bytes", 400);

        // store avatar
		ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
		int readBytes;
		byte[] bytes = new byte[1024];
		while ((readBytes = uploadedInputStream.read(bytes, 0, bytes.length)) != -1) {
			imageBuffer.write(bytes, 0, readBytes);
		}
		imageBuffer.flush();

		Avatar avatar = new Avatar(imageBuffer.toByteArray(), mediaType);
		long avatarId = avatarManager.addAvatar(avatar).getId();

        // update user
        String avatarUrl = UriBuilder.fromUri(uriInfo.getAbsolutePath()).path(String.valueOf(avatarId)).build().toString();
        UserDescription updatedUser = new UserDescription(null, null, null, null, null, null, null, null, avatarUrl);
        return userManager.updateUser(user, updatedUser);
    }


    @GET
    @Path("/{avatarId}")
    @Produces({"image/png", "image/gif", "image/jpg", "image/jpeg"})
    @UnitOfWork
    public Response getProfile(@PathParam("avatarId") long avatarId) {
        final Optional<Avatar> avatar = avatarManager.findAvatarById(avatarId);
        if (!avatar.isPresent()) throw RestUtils.createNotFoundException();

        return Response.ok()
                .entity(
                        new StreamingOutput() {
                            @Override
                            public void write(OutputStream output) throws IOException, WebApplicationException {
                                output.write(avatar.get().getContent());
                                output.flush();
                            }
                        })
                .type(avatar.get().getMediaType())
                .build();
    }

}
