package pl.verCodeEventSwiateczny.utils;

import java.util.List;
import java.util.stream.Collectors;

public class Chat {

    public static String color(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }

    public static List<String> colorLore(List<String> lore) {
        return lore.stream().map(Chat::color).collect(Collectors.toList());
    }
}
