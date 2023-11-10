package com.example.urlshortener.service;

import com.example.urlshortener.model.Tier;
import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlDto;
import com.example.urlshortener.model.User;
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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UrlService {

    @Autowired
    private BigtableDataClient dataClient;

    @Autowired
    private UserService userService;

    @Value("${URL_TABLE_ID}")
    private String urlTableId;

    @Value("${USER_TABLE_ID}")
    private String userTableId;

    private final String COLUMN_FAMILY_URL = "url_details";

    private final String COLUMN_FAMILY_TIME = "time_details";

    private final String COLUMN_FAMILY_CLICK = "click_details";

    private final String COLUMN_FAMILY_USER_URLS = "user_urls_details";

    public UrlService() { }

    public Url generateShortUrl(UrlDto url, String userId) {
        if (StringUtils.isNotEmpty(url.getUrl())) {
            User user = userService.getUserById(userId);
            Tier tier = user.getTier();
            Url shortenedUrl;

            String shortenedStr = shortenUrl(url.getUrl());
            shortenedUrl = new Url();
            shortenedUrl.setCreateDate(LocalDateTime.now());
            shortenedUrl.setLongUrl(url.getUrl());
            shortenedUrl.setShortUrl(shortenedStr);

            switch (tier){
                case BRONZE -> shortenedUrl.setExpireDate(shortenedUrl.getCreateDate().plusDays(30));
                case SILVER -> shortenedUrl.setExpireDate(shortenedUrl.getCreateDate().plusYears(1));
                case GOLD -> shortenedUrl.setExpireDate(shortenedUrl.getCreateDate().plusYears(1000));
            }
            shortenedUrl.setExpireDate(getExpireDate(url.getExpireDate(), shortenedUrl.getCreateDate()));
            saveUrlToDB(shortenedUrl);

            List<String> urls = user.getUrls()==null ? new ArrayList<>() : user.getUrls();
            urls.add(shortenedUrl.getShortUrl());

            RowMutation rowMutation = RowMutation.create(userTableId, userId)
                    .deleteCells(COLUMN_FAMILY_USER_URLS, "urls")
                    .setCell(COLUMN_FAMILY_USER_URLS, "urls", urls.toString());

            dataClient.mutateRow(rowMutation);
            return shortenedUrl;
        }
        return null;
    }

    public Url generateShortUrl(UrlDto url) {
        if (StringUtils.isNotEmpty(url.getUrl())) {
            String shortenedStr = shortenUrl(url.getUrl());
            Url shortenedUrl = new Url();
            shortenedUrl.setCreateDate(LocalDateTime.now());
            shortenedUrl.setLongUrl(url.getUrl());
            shortenedUrl.setShortUrl(shortenedStr);
            shortenedUrl.setExpireDate(getExpireDate(url.getExpireDate(), shortenedUrl.getCreateDate()));

            saveUrlToDB(shortenedUrl);
            return shortenedUrl;
        }
        return null;
    }

    private void saveUrlToDB(Url shortenedUrl) {

        String rowkey = shortenedUrl.getShortUrl();
        RowMutation rowMutation = RowMutation.create(urlTableId, rowkey)
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
    }

    private Row getRowByKey(String shortUrl) {
        try {
            Row row = dataClient.readRow(urlTableId, shortUrl);

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
            long clicksNAM = new BigInteger(clicksNAMCells.get(0).getValue().toByteArray()).longValue();
            RowMutation rowMutation = RowMutation.create(urlTableId, shortUrl)
                    .deleteCells(COLUMN_FAMILY_CLICK, "clicksNAM")
                    .setCell(COLUMN_FAMILY_CLICK, "clicksNAM",
                            clicksNAM+1);

            dataClient.mutateRow(rowMutation);
        }

        return longUrl;
    }


    public void deleteUrlById(String shortUrl, String userId) {
        RowMutation rowMutation = RowMutation.create(urlTableId, shortUrl).deleteRow();
        dataClient.mutateRow(rowMutation);

        userService.deleteUrlInUser(shortUrl, userId);
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
            Row row = dataClient.readRow(urlTableId, shortUrl);

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

    public Url getUrlById(String shorturl) {
        Row row = dataClient.readRow(urlTableId, shorturl);
        if (row == null)
            return null;
        Url url = new Url();
        url.setShortUrl(shorturl);
        System.out.println("Row: " + row.getKey().toStringUtf8());
        for (RowCell cell : row.getCells()) {
            String col = cell.getQualifier().toStringUtf8();
            switch (col) {
                case "longUrl":
                    url.setLongUrl(cell.getValue().toStringUtf8());
                    break;
                case "createDate":
                    String dateTime = cell.getValue().toStringUtf8();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    url.setCreateDate(LocalDateTime.parse(dateTime, formatter));
                    break;
                case "expireDate":
                    dateTime = cell.getValue().toStringUtf8();
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    url.setExpireDate(LocalDateTime.parse(dateTime, formatter));
                    break;
                case "clicksNAM":
                    url.setClicksNAM(new BigInteger(cell.getValue().toByteArray()).longValue());
                    break;
                case "clicksEMEA":
                    url.setClicksEMEA(new BigInteger(cell.getValue().toByteArray()).longValue());
                    break;
                case "clicksAPAC":
                    url.setClicksAPAC(new BigInteger(cell.getValue().toByteArray()).longValue());
                    break;
            }
            System.out.printf(
                    "Family: %s    Qualifier: %s    Value: %s%n",
                    cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8());
        }
        return url;
    }

    public List<Url> getUrlsByUser(String userId) {
        User user = userService.getUserById(userId);
        if(user==null) return null;
        List<String> shortUrls = user.getUrls();

        List<Url> urls = new ArrayList<>();
        if(shortUrls==null) return urls;

        for(String url: shortUrls){
            if(getUrlById(url)!=null) urls.add(getUrlById(url));
        }
        return urls;
    }
}
