package com.example.urlshortener.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.urlshortener.model.Tier;
import com.example.urlshortener.model.User;
import com.example.urlshortener.model.exception.BadRequestException;
import com.example.urlshortener.model.exception.ConflictException;
import com.example.urlshortener.model.exception.NotFoundException;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.*;
import com.google.common.hash.Hashing;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private BigtableDataClient dataClient;

    @Autowired
    private JwtService jwtService;

    @Value("${USER_TABLE_ID}")
    private String tableId;

    private final String COLUMN_FAMILY_USER = "user_details";

    private final String COLUMN_FAMILY_USER_URLS = "user_urls_details";

    public UserService() { }

    public User createUser(User user) {
        if(getUserByEmail(user.getEmail())!=null) throw new ConflictException("This email has been used already.");

        String rowkey = new User().getId().toString();
        RowMutation rowMutation = RowMutation.create(tableId, rowkey)
                .deleteCells(COLUMN_FAMILY_USER, "email")
                .setCell(COLUMN_FAMILY_USER, "email", user.getEmail())
                .deleteCells(COLUMN_FAMILY_USER, "password")
                .setCell(COLUMN_FAMILY_USER, "password",
                        Hashing.sha256().hashString(user.getPassword(), StandardCharsets.UTF_8).toString())
                .deleteCells(COLUMN_FAMILY_USER, "tier")
                .setCell(COLUMN_FAMILY_USER, "tier", Tier.BRONZE.name());

        dataClient.mutateRow(rowMutation);
        return getUserById(rowkey);
    }

    public User getUserById(String id) {
        Row row = dataClient.readRow(tableId, id);
        if (row == null)
            return null;
        User user = new User(UUID.fromString(row.getKey().toStringUtf8()));
        System.out.println("Row: " + row.getKey().toStringUtf8());
        for (RowCell cell : row.getCells()) {
            String col = cell.getQualifier().toStringUtf8();
            switch (col) {
                case "email":
                    user.setEmail(cell.getValue().toStringUtf8());
                    break;
                case "password":
                    user.setPassword(cell.getValue().toStringUtf8());
                    break;
                case "urls":
                    String urls = cell.getValue().toStringUtf8().replace("[","").replace("]","").replaceAll("\\s", "");
                    user.setUrls(urls=="" ? null : new ArrayList<String>(Arrays.asList(urls.split(","))));
                    break;
                case "tier":
                    user.setTier(Tier.valueOf(cell.getValue().toStringUtf8()));
                    break;
                case "tier_expire":
                    String dateTime = cell.getValue().toStringUtf8();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    user.setTierExpire(LocalDateTime.parse(dateTime, formatter));
                    break;
                case "area":
                    user.setArea(cell.getValue().toStringUtf8());
                    break;
            }
            System.out.printf(
                    "Family: %s    Qualifier: %s    Value: %s%n",
                    cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
        }
        return user;
    }

    public User updateSubscription(String userId, String tier) {

        if (getUserById(userId) == null){
            throw new NotFoundException("Not Found, User " + userId + " does not exist.");
        }

        if(!tier.equals(Tier.BRONZE.name()) && !tier.equals(Tier.SILVER.name()) && !tier.equals(Tier.GOLD.name())){
            throw new BadRequestException("Tier name not valid (BRONZE, SILVER or GOLD only).");
        }

        RowMutation rowMutation = RowMutation.create(tableId, userId)
                .deleteCells(COLUMN_FAMILY_USER, "tier")
                .setCell(COLUMN_FAMILY_USER, "tier", tier)
                .deleteCells(COLUMN_FAMILY_USER, "tier_expire")
                .setCell(COLUMN_FAMILY_USER, "tier_expire",
                        LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        dataClient.mutateRow(rowMutation);
        return getUserById(userId);
    }

    public User getUserByEmail(String email) {
        Filters.Filter emailFilter = Filters.FILTERS.chain()
                .filter(Filters.FILTERS.family().regex("user_details"))
                .filter(Filters.FILTERS.qualifier().regex("email"))
                .filter(Filters.FILTERS.value().regex(email));
        Query query = Query.create(tableId).filter(emailFilter);
        ServerStream<Row> rows = dataClient.readRows(query);
        for (Row row : rows) {
            String userId = row.getKey().toStringUtf8();
            return getUserById(userId);
        }
        throw new NotFoundException("Not Found, Email " + email + " does not exist.");
    }

    public String userLoggedIn(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    try {
                        DecodedJWT decode = jwtService.verifyToken(cookie.getValue());
                        return decode.getSubject();
                    } catch (Exception e) {
                        // Token is invalid or expired, proceed with login logic
                        return "";
                    }
                }
            }
        }
        return "";
    }

    public void deleteUrlInUser(String shorturl, String userId){
        User user = getUserById(userId);
        if(!user.getUrls().contains(shorturl)){
            return;
        }

        List<String> urls = user.getUrls();
        urls.remove(shorturl);
        user.setUrls(urls);

        RowMutation rowMutation = RowMutation.create(tableId, userId)
                .deleteCells(COLUMN_FAMILY_USER_URLS, "urls")
                .setCell(COLUMN_FAMILY_USER_URLS, "urls", urls.toString());

        dataClient.mutateRow(rowMutation);
    }

    public void deleteUser(String userId) {
        User user = getUserById(userId);
        if(user==null) throw new NotFoundException("Not Found, User " + userId + " does not exist.");

        RowMutation rowMutation = RowMutation.create(tableId, userId).deleteRow();
        dataClient.mutateRow(rowMutation);
    }
}
