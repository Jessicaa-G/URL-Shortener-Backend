package com.example.urlshortener.service;

import com.example.urlshortener.model.User;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class UserService {

    private BigtableDataClient dataClient;

    @Value("${USER_TABLE_ID}")
    private String tableId;

    private final String COLUMN_FAMILY_USER="user_details";

    public UserService(@Value("${PROJECT_ID}") final String projectId, @Value("${INSTANCE_ID}") final String instanceId) throws IOException {
        BigtableDataSettings settings = BigtableDataSettings.newBuilder().setProjectId(projectId).setInstanceId(instanceId).build();
        this.dataClient = BigtableDataClient.create(settings);
    }

    public User createUser(User user) {
        String rowkey = new User().getId().toString();

        RowMutation rowMutation = RowMutation.create(tableId, rowkey)
                .deleteCells(COLUMN_FAMILY_USER, "email")
                .setCell(COLUMN_FAMILY_USER, "email", user.getEmail())
                .deleteCells(COLUMN_FAMILY_USER, "password")
                .setCell(COLUMN_FAMILY_USER, "password", Hashing.sha256().hashString(user.getPassword(), StandardCharsets.UTF_8).toString());

        dataClient.mutateRow(rowMutation);
        return getUserById(rowkey);
    }

    private User getUserById(String id) {
        Row row = dataClient.readRow(tableId, id);
        if(row==null) return null;
        User user = new User(UUID.fromString(row.getKey().toStringUtf8()));
        System.out.println("Row: " + row.getKey().toStringUtf8());
        for (RowCell cell : row.getCells()) {
            String col = cell.getQualifier().toStringUtf8();
            switch (col){
                case "email":
                    user.setEmail(cell.getValue().toStringUtf8());
                    break;
                case "password":
                    user.setPassword(cell.getValue().toStringUtf8());
                    break;
                case "tier":
                    user.setTier(cell.getValue().toStringUtf8());
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

        if(getUserById(userId)==null) return null;

        RowMutation rowMutation = RowMutation.create(tableId, userId)
                .deleteCells(COLUMN_FAMILY_USER, "tier")
                .setCell(COLUMN_FAMILY_USER, "tier", tier)
                .deleteCells(COLUMN_FAMILY_USER, "tier_expire")
                .setCell(COLUMN_FAMILY_USER, "tier_expire", LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        dataClient.mutateRow(rowMutation);
        return getUserById(userId);
    }
}
