import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;

public class Main {

    private static JTextArea poem_editor;
    private static File current_poem_file;

    // Class for the import book function. Defines the two components of a poem.
    private static class ImportedPoem {

        String title;
        String text;

        ImportedPoem(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    // Class for the import book function. Defines the two components of a chapter.
    private static class ImportedChapter {

        String title;
        java.util.List<ImportedPoem> poems;

        ImportedChapter(String title, java.util.List<ImportedPoem> poems) {
            this.title = title;
            this.poems = poems;
        }
    }

    public static void main(String[] args) {

        // Sets up the structure of the application window.
        JFrame window = new JFrame("Poetry Book");
        window.setSize(1000, 700);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        JPanel poem_content_window = create_content_window();
        JPanel poem_directory = create_directory();
        JSplitPane split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, poem_directory, poem_content_window);
        split_pane.setDividerLocation(250);
        window.add(split_pane);
        window.setVisible(true);

    }

    private static JPanel create_directory() {
        DefaultMutableTreeNode book_node = new DefaultMutableTreeNode(read_book_title()); // Create root of GUI tree that will be used to store and organize chapters and individual poems

        // Create a File object representing the root book directory
        File book_folder = get_book_folder();
        if (!book_folder.exists()) {book_folder.mkdir();}
        File[] chapter_folders = book_folder.listFiles();

        java.util.List<String> chapter_order = read_order(book_folder); // Get the order of the chapters

        // Read the chapter and poem directories and create their GUI tree nodes using the txt order
        if (chapter_order != null) {
            for (String chapter_name : chapter_order) {

                File chapter_folder = new File(book_folder, chapter_name);

                if (chapter_folder.isDirectory()) {
                    load_chapter(book_node, chapter_folder);
                }
            }

        } else {

            if (chapter_folders != null) {
                Arrays.sort(chapter_folders);

                for (File chapter_folder : chapter_folders) {
                    if (chapter_folder.isDirectory()) {
                        load_chapter(book_node, chapter_folder);
                    }
                }
            }
        }

        JTree book_tree = new JTree(book_node);

        // Manager for nodes in the tree
        DefaultTreeModel tree_model = (DefaultTreeModel) book_tree.getModel();

        // Whatever is selected in the directory will be displayed in the poem editor
        book_tree.addTreeSelectionListener((TreeSelectionEvent event) -> load_poem(book_tree));

        JPanel directory_panel = new JPanel();
        directory_panel.setLayout(new java.awt.BorderLayout());

        JScrollPane tree_scroll_pane = new JScrollPane(book_tree);

        JButton new_chapter_button = new JButton("New Chapter");
        JButton new_poem_button = new JButton("New Poem");
        JButton rename_button = new JButton("Rename");
        JButton delete_button = new JButton("Delete");
        JButton move_up_button = new JButton("Move Up");
        JButton move_down_button = new JButton("Move Down");
        JButton move_poem_button = new JButton("Move Poem");
        JButton export_book_button = new JButton("Export Book");
        JButton new_book_button = new JButton("New Book");
        JButton import_book_button = new JButton("Import Book");

        // Copy book button customization
        export_book_button.setForeground(new java.awt.Color(255, 0, 0));

        JPanel button_panel = new JPanel(new java.awt.GridLayout(0, 2));
        button_panel.add(new_chapter_button);
        button_panel.add(new_poem_button);
        button_panel.add(rename_button);
        button_panel.add(delete_button);
        button_panel.add(move_up_button);
        button_panel.add(move_down_button);
        button_panel.add(move_poem_button);
        button_panel.add(export_book_button);
        button_panel.add(new_book_button);
        button_panel.add(import_book_button);

        new_chapter_button.addActionListener(event -> create_new_chapter(book_node, tree_model));
        new_poem_button.addActionListener(event -> create_new_poem(book_tree, tree_model));
        rename_button.addActionListener(event -> rename_item(book_tree, tree_model));
        delete_button.addActionListener(event -> delete_item(book_tree, tree_model));
        move_up_button.addActionListener(event -> move_item(book_tree, tree_model, -1));
        move_down_button.addActionListener(event -> move_item(book_tree, tree_model, 1));
        move_poem_button.addActionListener(event -> move_poem(book_tree, tree_model));
        export_book_button.addActionListener(event -> export_book());
        new_book_button.addActionListener(event -> new_book(book_node, tree_model));
        import_book_button.addActionListener(event -> import_book(book_node, tree_model));

        // Set location of chapter button and chapter tree upon the directory panel
        directory_panel.add(tree_scroll_pane, java.awt.BorderLayout.CENTER);
        directory_panel.add(button_panel, java.awt.BorderLayout.SOUTH);

        return directory_panel;
    }

    private static void load_poem(JTree book_tree) {

        // Find what node the user selected (if they, in fact, selected a node)
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();
        if (selected_node == null) {return;}

        // First, make sure that the program will only try to display content if a poem is selected
        // (Poems will always be on level 2)
        // Then, create a File object that represents the location of the poem the user clicks
        if (selected_node.getLevel() == 2) {
            DefaultMutableTreeNode chapter_node = (DefaultMutableTreeNode) selected_node.getParent();

            String chapter_name = chapter_node.toString();
            String poem_name = selected_node.toString();

            File poem_file = new File(get_book_folder(), chapter_name + "/" + poem_name + "/poem.txt");

            current_poem_file = poem_file;

            if (poem_file.exists()) {
                try {
                    String poem_text = java.nio.file.Files.readString(poem_file.toPath());
                    poem_editor.setText(poem_text);

                } catch (java.io.IOException e) {
                    e.printStackTrace();

                }
            }
        }
    }

    private static void create_new_chapter(DefaultMutableTreeNode book_node, DefaultTreeModel tree_model) {
        String chapter_name = JOptionPane.showInputDialog("Enter chapter name:");

        // Add new File obj representation for the chapter folder, and create it in the actual directory
        if (chapter_name != null && !chapter_name.isBlank()) {
            File chapter_folder = new File(get_book_folder(), chapter_name);

            if (!chapter_folder.exists()) {
                boolean folder_created = chapter_folder.mkdir(); // Create new folder, then save if the operation worked

                if (folder_created) {
                    DefaultMutableTreeNode new_chapter_node = new DefaultMutableTreeNode(chapter_name);
                    tree_model.insertNodeInto(new_chapter_node, book_node, book_node.getChildCount());
                    save_order(book_node, get_book_folder());
                }
            }
        }
    }

    private static void create_new_poem(JTree book_tree, DefaultTreeModel tree_model) {
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();

        // Make sure a chapter is selected so there is a place to put the folder
        if (selected_node == null || selected_node.getLevel() != 1) {
            JOptionPane.showMessageDialog(null, "Please select a chapter first.");
            return;
        }

        String poem_name = JOptionPane.showInputDialog("Enter poem name:");
        if (poem_name == null || poem_name.isBlank()) {return;}

        String chapter_name = selected_node.toString();

        File new_poem_folder = new File(get_book_folder(), chapter_name + "/" + poem_name);

        if (new_poem_folder.exists()) {
            JOptionPane.showMessageDialog(null, "A poem with that name already exists.");
            return;
        }

        boolean folder_created = new_poem_folder.mkdir();
        if (!folder_created) {return;}

        File poem_file = new File(new_poem_folder, "poem.txt");
        try {
            poem_file.createNewFile();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;

        }

        DefaultMutableTreeNode new_poem_node = new DefaultMutableTreeNode(poem_name);
        tree_model.insertNodeInto(new_poem_node, selected_node, selected_node.getChildCount());
        save_order(selected_node, new File(get_book_folder(), chapter_name));
    }

    private static void rename_item(JTree book_tree, DefaultTreeModel tree_model) {
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();

        if (selected_node == null) {
            JOptionPane.showMessageDialog(null, "Please select a chapter or poem first.");
            return;
        }

        // Prevent renaming book. This is a flaw, but I will fix it eventually.
        if (selected_node.getLevel() == 0) {
            JOptionPane.showMessageDialog(null,"The book itself cannot be renamed.");
            return;
        }

        String item_type; // Stores if you are renaming chapter or poem

        // Determine whether a chapter or poem is being renamed
        if (selected_node.getLevel() == 1) {
            item_type = "chapter";
        } else {
            item_type = "poem";
        }

        String current_name = selected_node.toString(); // Stores the current name of the poem the user wishes to rename

        String new_name = JOptionPane.showInputDialog("Enter new " + item_type + " name:", current_name);

        if (new_name == null || new_name.isBlank()) {return;}

        File current_folder;
        File renamed_folder;

        // Set old name and new name depending on whether a poem or chapter was selected
        if (selected_node.getLevel() == 1) {
            current_folder = new File(get_book_folder(), current_name);
            renamed_folder = new File(get_book_folder(), new_name);

        } else {
            DefaultMutableTreeNode chapter_node = (DefaultMutableTreeNode) selected_node.getParent();
            String chapter_name = chapter_node.toString();
            current_folder = new File(get_book_folder(), chapter_name + "/" + current_name);
            renamed_folder = new File(get_book_folder(), chapter_name + "/" + new_name);
        }

        boolean renamed = current_folder.renameTo(renamed_folder);  // Stores whether or not the item was renamed, and renames filesystem title

        if (!renamed) {
            JOptionPane.showMessageDialog(null, "Unable to rename " + item_type + ".");
            return;
        }

        // Change current file variable to updated name
        if (selected_node.getLevel() == 2) {
            current_poem_file = new File(renamed_folder, "poem.txt");
        } else {
            current_poem_file = null;
            poem_editor.setText("");
        }  // If a chapter is renamed, just wait until new thing is selected and it will update

        selected_node.setUserObject(new_name);  // Change display name
        tree_model.nodeChanged(selected_node);  // Refresh display with new name

        // Update order txt files with renamed file
        DefaultMutableTreeNode parent_node = (DefaultMutableTreeNode) selected_node.getParent();
        File parent_folder;
        if (selected_node.getLevel() == 1) {
            parent_folder = get_book_folder();

        } else {
            String chapter_name = parent_node.toString();
            parent_folder = new File(get_book_folder(), chapter_name);
        }

        save_order(parent_node, parent_folder);

    }

    private static void delete_item(JTree book_tree, DefaultTreeModel tree_model) {
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();

        if (selected_node == null) {
            JOptionPane.showMessageDialog(null, "Please select a chapter or poem first.");
            return;
        }

        if (selected_node.getLevel() == 0) {
            JOptionPane.showMessageDialog(null, "The book itself cannot be deleted.");
            return;
        }

        String item_type;   // Stores if the user wants to delete a chapter or poem

        if (selected_node.getLevel() == 1) {
            item_type = "chapter";
        } else {
            item_type = "poem";
        }

        String item_name = selected_node.toString();    // Convert item name to string to get position in directory

        int response = JOptionPane.showConfirmDialog(null, "Are you SURE... you want to delete " + item_type + ": " + item_name, "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (response != JOptionPane.YES_OPTION) {return;}

        File item_folder;

        // Find and store position in directory
        if (selected_node.getLevel() == 1) {
            item_folder = new File(get_book_folder(), item_name);

        } else {

            DefaultMutableTreeNode chapter_node = (DefaultMutableTreeNode) selected_node.getParent();
            String chapter_name = chapter_node.toString();

            item_folder = new File(get_book_folder(), chapter_name + "/" + item_name);

        }

        boolean deleted = delete_folder(item_folder);

        if (!deleted) {
            JOptionPane.showMessageDialog(null, "Unable to delete " + item_type + ".");
            return;
        }

        // Get parent nodes before node is deleted
        DefaultMutableTreeNode parent_node = (DefaultMutableTreeNode) selected_node.getParent();
        File parent_folder;
        if (selected_node.getLevel() == 1) {
            parent_folder = get_book_folder();
        } else {
            String chapter_name = parent_node.toString();
            parent_folder = new File(get_book_folder(), chapter_name);
        }

        tree_model.removeNodeFromParent(selected_node);

        save_order(parent_node, parent_folder);

        // Remove left over poem contents from content window
        current_poem_file = null;
        poem_editor.setText("");

    }

    private static boolean delete_folder(File folder) {
        File[] contents = folder.listFiles();

        // Recursively deletes poem folders in chapter files, and the contents within the poem folders
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    if (!delete_folder(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }

        return folder.delete();
    }

    private static void move_item(JTree book_tree, DefaultTreeModel tree_model, int direction) {
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();

        if (selected_node == null) {return;}

        if (selected_node.getLevel() == 0) {return;}


        DefaultMutableTreeNode parent_node = (DefaultMutableTreeNode) selected_node.getParent();

        int current_position = parent_node.getIndex(selected_node);

        int new_position = current_position + direction;

        // Cancel if the position is already 0 or at the end
        if (new_position < 0 || new_position >= parent_node.getChildCount()) {
            return;
        }

        tree_model.removeNodeFromParent(selected_node);    // You have to remove the node to change its position
        tree_model.insertNodeInto(selected_node, parent_node, new_position);

        // Make sure the item moved is still selected
        book_tree.setSelectionPath(new javax.swing.tree.TreePath(selected_node.getPath()));

        File parent_folder;

        // Save new order to the directory that contains the items you are moving
        if (selected_node.getLevel() == 1) {
            parent_folder = get_book_folder();
        } else {
            String chapter_name = parent_node.toString();
            parent_folder = new File(get_book_folder(), chapter_name);
        }

        save_order(parent_node, parent_folder);
    }

    private static void move_poem(JTree book_tree, DefaultTreeModel tree_model) {
        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) book_tree.getLastSelectedPathComponent();

        if (selected_node == null || selected_node.getLevel() != 2) {
            JOptionPane.showMessageDialog(null, "You must select a poem.");
            return;
        }

        // Reference for current chapter and chapter order
        DefaultMutableTreeNode current_chapter = (DefaultMutableTreeNode) selected_node.getParent();
        java.util.List<String> saved_chapter_order = read_order(get_book_folder());

        if (saved_chapter_order == null) {
            JOptionPane.showMessageDialog(null, "Could not find the chapter order.");
            return;
        }

        java.util.List<String> chapter_names = new java.util.ArrayList<>(saved_chapter_order);
        chapter_names.remove(current_chapter.toString());

        // If there is only one chapter, make sure the user can't move the poem
        if (chapter_names.isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no other chapters to move this poem to.");
            return;
        }

        String destination_chapter = (String) JOptionPane.showInputDialog(null, "Move poem to which chapter?", "Move Poem", JOptionPane.QUESTION_MESSAGE, null, chapter_names.toArray(), null);

        if (destination_chapter == null) {return;}

        // Get name of poem to be moved and current chapter in which it is residing
        String poem_name = selected_node.toString();
        String current_chapter_name = current_chapter.toString();

        File old_poem_folder = new File(get_book_folder(), current_chapter_name + "/" + poem_name);
        File new_poem_folder = new File(get_book_folder(), destination_chapter + "/" + poem_name);

        if (new_poem_folder.exists()) {
            JOptionPane.showMessageDialog(null, "Two poems with the same name cannot exist in one chapter.");
            return;
        }

        // Move the poem folder to the new chapter (rename to can actually change the file path)
        boolean folder_moved = old_poem_folder.renameTo(new_poem_folder);

        if (!folder_moved) {
            JOptionPane.showMessageDialog(null, "The poem could not be moved.");
            return;
        }

        // Update the poem txt file location as well as the folder
        current_poem_file = new File(new_poem_folder, "poem.txt");

        // Find the destination chapter node in the tree
        DefaultMutableTreeNode book_node = (DefaultMutableTreeNode) current_chapter.getParent();
        DefaultMutableTreeNode destination_chapter_node = null;

        // Search through the nodes and find the node that shares the same name as the destination chapter
        for (int i = 0; i < book_node.getChildCount(); i++) {
            DefaultMutableTreeNode chapter_node = (DefaultMutableTreeNode) book_node.getChildAt(i);

            if (chapter_node.toString().equals(destination_chapter)) {
                destination_chapter_node = chapter_node;
                break;
            }
        }

        // Remove the poem from its old chapter
        tree_model.removeNodeFromParent(selected_node);

        // Add the poem to the end of the destination chapter
        tree_model.insertNodeInto(selected_node, destination_chapter_node, destination_chapter_node.getChildCount());

        // Update the order file for the old chapter
        File old_chapter_folder = new File(get_book_folder(), current_chapter_name);
        save_order(current_chapter, old_chapter_folder);

        // Update the order file for the destination chapter
        File new_chapter_folder = new File(get_book_folder(), destination_chapter);
        save_order(destination_chapter_node, new_chapter_folder);

        // Keep the moved poem selected
        book_tree.setSelectionPath(new javax.swing.tree.TreePath(selected_node.getPath()));

    }

    private static JPanel create_content_window() {

        JPanel content_panel = new JPanel();
        content_panel.setLayout(new java.awt.BorderLayout());

        poem_editor = new JTextArea();
        poem_editor.setFont(new java.awt.Font("Serif", java.awt.Font.PLAIN,18));
        poem_editor.setLineWrap(true);
        poem_editor.setWrapStyleWord(true);

        JScrollPane editor_scroll_pane = new JScrollPane(poem_editor);
        JButton save_button = new JButton("Save");

        save_button.addActionListener(event -> save_poem());

        content_panel.add(editor_scroll_pane, java.awt.BorderLayout.CENTER);
        content_panel.add(save_button, java.awt.BorderLayout.SOUTH);

        return content_panel;

    }

    private static void save_poem() {
        if (current_poem_file != null) {

            try {
                java.nio.file.Files.writeString(
                        current_poem_file.toPath(),
                        poem_editor.getText()
                );

            } catch (java.io.IOException e) {
                e.printStackTrace();

            }
        }
    }

    private static void save_order(DefaultMutableTreeNode parent_node, File parent_folder) {

        StringBuilder order_text = new StringBuilder(); // Where the order of the directory/subdirectory will be stored

        // Add each subdirectory in order to the string
        for (int i = 0; i < parent_node.getChildCount(); i++) {
            DefaultMutableTreeNode child_node = (DefaultMutableTreeNode) parent_node.getChildAt(i);
            order_text.append(child_node.toString());
            order_text.append(System.lineSeparator());
        }

        File order_file = new File(parent_folder, "order.txt"); // The order will be stored in a txt file

        // Write the order to the order txt file
        try {
            java.nio.file.Files.writeString(order_file.toPath(), order_text.toString());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static void load_poems(DefaultMutableTreeNode chapter_node, File chapter_folder) {
        java.util.List<String> poem_order = read_order(chapter_folder);

        // Check if there is an order
        if (poem_order != null) {
            // Add poems in order
            for (String poem_name : poem_order) {
                File poem_folder = new File(chapter_folder, poem_name);

                if (poem_folder.isDirectory()) {
                    DefaultMutableTreeNode poem_node = new DefaultMutableTreeNode(poem_folder.getName());
                    chapter_node.add(poem_node);
                }
            }
        } else {
            File[] poem_folders = chapter_folder.listFiles();

            if (poem_folders != null) {
                // Add poems in alphabetical order
                Arrays.sort(poem_folders);

                for (File poem_folder : poem_folders) {
                    if (poem_folder.isDirectory()) {
                        DefaultMutableTreeNode poem_node = new DefaultMutableTreeNode(poem_folder.getName());
                        chapter_node.add(poem_node);
                    }
                }
            }
        }
    }

    private static void load_chapter(DefaultMutableTreeNode book_node, File chapter_folder) {
        DefaultMutableTreeNode chapter_node = new DefaultMutableTreeNode(chapter_folder.getName());

        load_poems(chapter_node, chapter_folder);
        book_node.add(chapter_node);

    }

    private static java.util.List<String> read_order(File parent_folder) {

        // Create location representation of order txt file
        File order_file = new File(parent_folder, "order.txt");

        // Return nothing if there isn't an order (this is really badddd)
        if (!order_file.exists()) {return null;}

        try {
            return java.nio.file.Files.readAllLines(order_file.toPath());
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void export_book() {
        String book_rtf = create_book_rtf();

        JFileChooser file_chooser = new JFileChooser();
        file_chooser.setDialogTitle("Export Book");
        file_chooser.setSelectedFile(new File("Poetry Book.rtf"));  // Sets file name

        int result = file_chooser.showSaveDialog(null); // Displays save dialogue

        if (result == JFileChooser.APPROVE_OPTION) {
            File export_file = file_chooser.getSelectedFile();

            try {
                java.nio.file.Files.writeString(export_file.toPath(), book_rtf);
                JOptionPane.showMessageDialog(null, "Book exported successfully.");

            } catch (java.io.IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Unable to export book.");
            }
        }
    }

    private static String create_book_rtf() {
        StringBuilder book_rtf = new StringBuilder();   // Where the book will be stored

        // Start RTF doc and make Times New Roman default font
        book_rtf.append("{\\rtf1\\ansi\n");
        book_rtf.append("{\\fonttbl{\\f0 Times New Roman;}}\n");
        book_rtf.append("\\f0\n");

        // Add title page as 48 point, bold, centered
        book_rtf.append("\\qc\\b\\fs96 ");
        book_rtf.append(escape_rtf(read_book_title()));
        book_rtf.append("\\b0\\par\n");

        // Start first chapter on a new page
        book_rtf.append("\\page\n");

        // Get chapters of the book
        File book_folder = get_book_folder();
        java.util.List<String> chapter_order = read_order(book_folder);

        // Make sure chapters exist
        if (chapter_order != null) {

            // Add chapters
            for (String chapter_name : chapter_order) {
                File chapter_folder = new File(book_folder, chapter_name);

                // Add chapter title as 32 point, bold, centered
                book_rtf.append("\\qc\\b\\fs64 ");
                book_rtf.append(escape_rtf(chapter_name));
                book_rtf.append("\\b0\\par\n");

                // Start poems on a new page
                book_rtf.append("\\page\n");

                // Get the order of poems in the chapter
                java.util.List<String> poem_order = read_order(chapter_folder);

                // Add the poems for each chapter
                if (poem_order != null) {
                    for (String poem_name : poem_order) {
                        File poem_folder = new File(chapter_folder, poem_name);
                        File poem_file = new File(poem_folder, "poem.txt");

                        // Make sure a poem txt file exists in the poem folder
                        if (poem_file.exists()) {
                            try {
                                String poem_text =
                                        java.nio.file.Files.readString(poem_file.toPath());

                                // Add poem title as 18 point, bold, centered
                                book_rtf.append("\\qc\\b\\fs36 ");
                                book_rtf.append(escape_rtf(poem_name));
                                book_rtf.append("\\b0\\par\n");

                                // Add blank line
                                book_rtf.append("\\fs24\\par\n");

                                // Add poem text as 12 point, normal, left aligned
                                book_rtf.append("\\ql\\fs24 ");
                                book_rtf.append(escape_rtf(poem_text));
                                book_rtf.append("\\par\n");

                                // Start the next poem on a new page
                                book_rtf.append("\\page\n");

                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        // End RTF document
        book_rtf.append("}");
        return book_rtf.toString();
    }

    private static String read_book_title() {

        File title_file = new File(get_book_folder(), "title.txt");

        if (title_file.exists()) {
            try {
                return java.nio.file.Files.readString(title_file.toPath()).trim();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return "BOOK";
    }

    private static String escape_rtf(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\line\n");
    }

    private static String unescape_rtf(String text) {

        return text
                .replace("\\line\r\n", "\n")
                .replace("\\line\n", "\n")
                .replace("\\line", "\n")
                .replace("\\}", "}")
                .replace("\\{", "{")
                .replace("\\\\", "\\");
    }

    private static void new_book(DefaultMutableTreeNode book_node, DefaultTreeModel tree_model) {

        int choice = JOptionPane.showConfirmDialog(null, "This will permanently delete all chapters and poems.\n\n" + "Are you sure you want to start a new book?", "New Book", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {return;}

        String book_title = JOptionPane.showInputDialog(null, "Enter the title of the new book:", "New Book", JOptionPane.PLAIN_MESSAGE);

        if (book_title == null) {return;}

        File book_folder = get_book_folder();

        delete_folder(book_folder);

        // Create a new empty book folder
        book_folder.mkdir();

        // Create location representation of book title txt
        File title_file = new File(book_folder, "title.txt");

        try {
            java.nio.file.Files.writeString(title_file.toPath(), book_title);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Update book title
        book_node.setUserObject(book_title);

        // Clear chapters and poems from the tree
        book_node.removeAllChildren();
        tree_model.reload();

        // Clear the poem editor
        poem_editor.setText("");
        current_poem_file = null;
    }

    private static void import_book(DefaultMutableTreeNode book_node, DefaultTreeModel tree_model) {

        // Variable that will ask to select file
        JFileChooser file_chooser = new JFileChooser();

        file_chooser.setDialogTitle("Import Book");

        int result = file_chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {

            // Ask user to select file
            File import_file = file_chooser.getSelectedFile();

            try {
                //Convert file into a string
                String book_rtf = java.nio.file.Files.readString(import_file.toPath());

                String imported_title = get_imported_book_title(book_rtf);
                java.util.List<ImportedChapter> imported_chapters = get_imported_book(book_rtf);

                int choice = JOptionPane.showConfirmDialog(null, "Import \"" + imported_title + "\"?\n\n" + "This will permanently replace the current book.", "Import Book", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice != JOptionPane.YES_OPTION) {return;}

                create_imported_book(imported_title, imported_chapters, book_node, tree_model);

                JOptionPane.showMessageDialog(null, "Book imported successfully.");

            } catch (java.io.IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Unable to read book file.");
            }
        }
    }

    private static String get_imported_book_title(String book_rtf) {

        // This is the RTF format for the title
        String title_start = "\\qc\\b\\fs96 ";
        String title_end = "\\b0\\par";

        int start_position = book_rtf.indexOf(title_start);

        // If there is no text, cancel
        if (start_position == -1) {return null;}

        // You know that the file will begin with the open rtf encoding, and then the title
        start_position = start_position + title_start.length();

        // Extract length of title
        int end_position = book_rtf.indexOf(title_end, start_position);

        // If there is an encoding but no title, cancel
        if (end_position == -1) {return null;}

        // Return title
        return unescape_rtf(book_rtf.substring(start_position, end_position));
    }

    private static java.util.List<ImportedChapter> get_imported_book(String book_rtf) {

        java.util.List<ImportedChapter> chapters = new java.util.ArrayList<>();

        String chapter_start = "\\qc\\b\\fs64 ";
        String chapter_end = "\\b0\\par";

        int search_position = 0;

        while (true) {

            // Find the beginning of the next chapter using the chapter start encoding
            int chapter_start_position = book_rtf.indexOf(chapter_start, search_position);

            // If there is no start
            if (chapter_start_position == -1) {break;}

            // Skip over the start encoding to get to the start of the title
            int title_start_position = chapter_start_position + chapter_start.length();

            // Get the end of the chapter title
            int title_end_position = book_rtf.indexOf(chapter_end, title_start_position);

            // If there is no end, break
            if (title_end_position == -1) {break;}

            // Get the chapter title
            String chapter_title = unescape_rtf(book_rtf.substring(title_start_position, title_end_position));

            // Find the beginning of the next chapter
            int next_chapter_position = book_rtf.indexOf(chapter_start, title_end_position);

            // Determine where this chapter's section ends
            int chapter_section_end;

            // Handles last chapter in book
            if (next_chapter_position == -1) {
                chapter_section_end = book_rtf.length();
            } else {
                chapter_section_end = next_chapter_position;
            }

            // Gets chapter's entire RTF section
            String chapter_rtf = book_rtf.substring(chapter_start_position, chapter_section_end);

            // Get poems belonging to this chapter
            java.util.List<ImportedPoem> poems = get_imported_poems(chapter_rtf);

            // Storechapter title and poems together
            ImportedChapter chapter = new ImportedChapter(chapter_title, poems);

            chapters.add(chapter);

            // Continue searching at the next chapter
            search_position = chapter_section_end;
        }

        return chapters;
    }

    private static java.util.List<ImportedPoem> get_imported_poems(String chapter_rtf) {

        java.util.List<ImportedPoem> poems = new java.util.ArrayList<>();

        // Start and end encodings for poem titles and poem text
        String poem_start = "\\qc\\b\\fs36 ";
        String poem_title_end = "\\b0\\par";
        String poem_text_start = "\\ql\\fs24 ";
        String poem_text_end = "\\par";

        int search_position = 0;

        while (true) {

            // Find beginning of upcoming poem title
            int title_start_position = chapter_rtf.indexOf(poem_start, search_position);

            // If there are no more titles (or none were ever there) break
            if (title_start_position == -1) {break;}

            // Go to beginning of actual title past start encoding
            title_start_position = title_start_position + poem_start.length();

            // Get the end position of the poem title
            int title_end_position = chapter_rtf.indexOf(poem_title_end, title_start_position);

            // If there is no end, break
            if (title_end_position == -1) {break;}

            // Extract the poem title
            String poem_title = unescape_rtf(chapter_rtf.substring(title_start_position, title_end_position));

            // Find beginning of the poem text using start encoding
            int text_start_position = chapter_rtf.indexOf(poem_text_start, title_end_position);

            // If there is no more poem text, break
            if (text_start_position == -1) {break;}

            // Move past the start encoding to the actual poem text
            text_start_position = text_start_position + poem_text_start.length();

            // Find the end of the poem text
            int text_end_position = chapter_rtf.indexOf(poem_text_end, text_start_position);

            // If there is no end encoding, break
            if (text_end_position == -1) {break;}

            // Extract the poem text
            String poem_text = unescape_rtf(chapter_rtf.substring(text_start_position, text_end_position));

            // Store the poem title and text together in ImportedPoem class
            ImportedPoem poem = new ImportedPoem(poem_title, poem_text);

            poems.add(poem);

            // Continue searching after this poem
            search_position = text_end_position + poem_text_end.length();
        }

        return poems;
    }

    private static void create_imported_book(String imported_title, java.util.List<ImportedChapter> imported_chapters, DefaultMutableTreeNode book_node, DefaultTreeModel tree_model) {

        File book_folder = get_book_folder();

        delete_folder(book_folder);

        book_folder.mkdir();

        // Create title.txt
        File title_file = new File(book_folder, "title.txt");

        // See if new title can be imported
        try {
            java.nio.file.Files.writeString(title_file.toPath(), imported_title);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Clear existing visual directory
        book_node.removeAllChildren();

        // Change displayed book title
        book_node.setUserObject(imported_title);

        // Create each chapter
        for (ImportedChapter chapter : imported_chapters) {

            // Creates file object representing the chapter
            File chapter_folder = new File(book_folder, chapter.title);

            // Makes the chapter folder
            chapter_folder.mkdir();

            // Creates tree representation of chapter
            DefaultMutableTreeNode chapter_node = new DefaultMutableTreeNode(chapter.title);

            book_node.add(chapter_node);

            // Create each poem in this chapter
            for (ImportedPoem poem : chapter.poems) {

                // Create file object representation of poem
                File poem_folder = new File(chapter_folder, poem.title);

                // Makes the poem folder
                poem_folder.mkdir();

                // Puts the poem txt in the poem file
                File poem_file = new File(poem_folder, "poem.txt");

                // Checks if the poem txt can be written to the poem file
                try {
                    java.nio.file.Files.writeString(poem_file.toPath(), poem.text);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }

                // Create tree representation of poem
                DefaultMutableTreeNode poem_node = new DefaultMutableTreeNode(poem.title);

                // Add poem to tree
                chapter_node.add(poem_node);
            }

            // Save poem order for this chapter
            save_order(chapter_node, chapter_folder);
        }

        // Save chapter order
        save_order(book_node, book_folder);

        // Refresh visual directory
        tree_model.reload();

        // Clear poem editor
        poem_editor.setText("");
        current_poem_file = null;
    }

    private static File get_book_folder() {

        // Get location for storing book
        File documents_folder = new File(System.getProperty("user.home"), "Documents");

        // Get location where books will eventually be placed
        File poetry_book_folder = new File(documents_folder, "Poetry Book");

        // Get location where book file will be placed
        File book_folder = new File(poetry_book_folder, "book");

        if (!book_folder.exists()) {
            book_folder.mkdirs();
        }

        System.out.println(book_folder.getAbsolutePath());

        return book_folder;
    }
}
