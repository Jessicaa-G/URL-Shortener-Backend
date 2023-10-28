package com.example.urlshortener.service;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlDto;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.ReadModifyWriteRow;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.common.hash.Hashing;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UrlService {

    @Autowired
    private BigtableDataClient dataClient;

    @Value("${URL_TABLE_ID}")
    private String tableId;

    private final String COLUMN_FAMILY_URL = "url_details";

    private final String COLUMN_FAMILY_TIME = "time_details";

    private final String COLUMN_FAMILY_CLICK = "click_details";

    public UrlService() { }

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
                            shortenedUrl.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .deleteCells(COLUMN_FAMILY_TIME, "expireDate")
                    .setCell(COLUMN_FAMILY_TIME, "expireDate",
                            shortenedUrl.getExpireDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
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
            getRowByKey(shortenedUrl.getShortUrl());
            return shortenedUrl;
        }
        return null;
    }

    private Row getRowByKey(String shortUrl) {
        try {
            Row row = dataClient.readRow(tableId, shortUrl);

            if (row != null) {
                System.out.println("Row: " + row.getKey().toStringUtf8());

                for (RowCell cell : row.getCells()) {
                    System.out.printf(
                            "Family: %s    Qualifier: %s    Value: %s%n",
                            cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
                }
                return row;
            } else {
                // Handle the case where the row with the given shortUrl is not found
                throw new RuntimeException("Row not found for " + shortUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving the Long URL");
        }
    }

    public String redirect(String shortUrl) {
        Row row = getRowByKey(shortUrl);
        List<RowCell> longUrlCells = row.getCells(COLUMN_FAMILY_URL, "longUrl");
        List<RowCell> clicksNAMCells = row.getCells(COLUMN_FAMILY_CLICK, "clicksNAM");

        String longUrl = null;

        if (!longUrlCells.isEmpty()) {
            longUrl = longUrlCells.get(0).getValue().toStringUtf8();
        }
        // TODO: make this regionally
        // Add click times
        if (!clicksNAMCells.isEmpty()) {
            ReadModifyWriteRow rowMutation = ReadModifyWriteRow.create(tableId, shortUrl)
                    .increment(COLUMN_FAMILY_CLICK, "clicksNAM", 1);
            dataClient.readModifyWriteRow(rowMutation);
        }

        return longUrl;
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

    public Map<String, Long> getStats(String shortUrl) {
        try {
            Row row = dataClient.readRow(tableId, shortUrl);

            if (row != null) {
                List<RowCell> clicksNAMCells = row.getCells(COLUMN_FAMILY_CLICK, "clicksNAM");
                List<RowCell> clicksEMEACells = row.getCells(COLUMN_FAMILY_CLICK, "clicksEMEA");
                List<RowCell> clicksAPACCells = row.getCells(COLUMN_FAMILY_CLICK, "clicksAPAC");
                if (!clicksNAMCells.isEmpty() && !clicksEMEACells.isEmpty() && !clicksAPACCells.isEmpty()) {
                    Map<String, Long> clicks = new HashMap<>();
                    clicks.put("NAM", ByteBuffer.wrap(clicksNAMCells.get(0).getValue().toByteArray()).getLong());
                    clicks.put("EMEA", ByteBuffer.wrap(clicksEMEACells.get(0).getValue().toByteArray()).getLong());
                    clicks.put("APAC", ByteBuffer.wrap(clicksAPACCells.get(0).getValue().toByteArray()).getLong());
                    return clicks;
                } else {
                    throw new RuntimeException("Clicks found for" + shortUrl);
                }
            } else {
                // Handle the case where the row with the given shortUrl is not found
                throw new RuntimeException("Row not found for " + shortUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving the Long URL");
        }
    }

}
