package com.example.urlshortener.service;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlDto;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.common.hash.Hashing;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class UrlService {

    private BigtableDataClient dataClient;

    @Value("${URL_TABLE_ID}")
    private String tableId;

    private final String COLUMN_FAMILY_URL = "url_details";

    private final String COLUMN_FAMILY_TIME = "time_details";

    private final String COLUMN_FAMILY_CLICK = "click_details";

    public UrlService(@Value("${PROJECT_ID}") final String projectId, @Value("${INSTANCE_ID}") final String instanceId)
            throws IOException {
        BigtableDataSettings settings = BigtableDataSettings.newBuilder().setProjectId(projectId)
                .setInstanceId(instanceId).build();
        this.dataClient = BigtableDataClient.create(settings);
    }

    public Url generateShortUrl(UrlDto url) {
        if (StringUtils.isNotEmpty(url.getUrl())) {
            String shortenedStr = shortenUrl(url.getUrl());
            Url shortenedUrl = new Url();
            shortenedUrl.setCreateDate(LocalDateTime.now());
            shortenedUrl.setLongUrl(url.getUrl());
            shortenedUrl.setShortUrl(shortenedStr);
            shortenedUrl.setExpireDate(getExpireDate(url.getExpireDate(), shortenedUrl.getCreateDate()));

            String rowkey = shortenedUrl.getShortUrl();
            RowMutation rowMutation = RowMutation.create(tableId, rowkey)
                    .deleteCells(COLUMN_FAMILY_URL, "longUrl")
                    .setCell(COLUMN_FAMILY_URL, "longUrl", shortenedUrl.getLongUrl())
                    // TODO: short url is already in rowkey
                    // .deleteCells(COLUMN_FAMILY_URL, "shortUrl")
                    // .setCell(COLUMN_FAMILY_URL, "shortUrl", shortenedUrl.getShortUrl())
                    .deleteCells(COLUMN_FAMILY_TIME, "createDate")
                    .setCell(COLUMN_FAMILY_TIME, "createDate",
                            shortenedUrl.getCreateDate().toEpochSecond(ZoneOffset.UTC))
                    .deleteCells(COLUMN_FAMILY_TIME, "expireDate")
                    .setCell(COLUMN_FAMILY_TIME, "expireDate",
                            shortenedUrl.getExpireDate().toEpochSecond(ZoneOffset.UTC))
                    .deleteCells(COLUMN_FAMILY_CLICK, "clicksNAM")
                    .setCell(COLUMN_FAMILY_CLICK, "clicksNAM",
                            shortenedUrl.getClicksNAM())
                    .deleteCells(COLUMN_FAMILY_CLICK, "clicksEMEA")
                    .setCell(COLUMN_FAMILY_CLICK, "clicksEMEA",
                            shortenedUrl.getClicksEMEA())
                    .deleteCells(COLUMN_FAMILY_CLICK, "clicksAPAC")
                    .setCell(COLUMN_FAMILY_CLICK, "clicksAPAC",
                            shortenedUrl.getClicksAPAC());

            dataClient.mutateRow(rowMutation);
            getLongUrlById(shortenedUrl.getShortUrl());
            return shortenedUrl;
        }
        return null;
    }

    public String getLongUrlById(String shortUrl) {
        try {
            Row row = dataClient.readRow(tableId, shortUrl);

            if (row != null) {
                System.out.println("Row: " + row.getKey().toStringUtf8());

                for (RowCell cell : row.getCells()) {
                    System.out.printf(
                            "Family: %s    Qualifier: %s    Value: %s%n",
                            cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
                }

                List<RowCell> longUrlCells = row.getCells("url_details", "longUrl");

                if (!longUrlCells.isEmpty()) {
                    return longUrlCells.get(0).getValue().toStringUtf8();
                } else {
                    // Handle the case where longUrl is not found in the row
                    throw new RuntimeException("Long URL not found for " + shortUrl);
                }
            } else {
                // Handle the case where the row with the given shortUrl is not found
                throw new RuntimeException("Row not found for " + shortUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving the Long URL");
        }
    }

    public void deleteUrlById(String shortUrl) {
        RowMutation rowMutation = RowMutation.create(tableId, shortUrl).deleteRow();
        dataClient.mutateRow(rowMutation);
    }

    private String shortenUrl(String url) {
        String shortenedUrl = "";
        LocalDateTime time = LocalDateTime.now();
        shortenedUrl = Hashing.murmur3_32()
                .hashString(url.concat(time.toString()), StandardCharsets.UTF_8)
                .toString();
        return shortenedUrl;
    }

    private LocalDateTime getExpireDate(String expireDate, LocalDateTime createDate) {
        if (StringUtils.isBlank(expireDate)) {
            // TODO: set expiration date according to user tier level
            return createDate.plusDays(30);
        }
        return LocalDateTime.parse(expireDate);
    }

}
