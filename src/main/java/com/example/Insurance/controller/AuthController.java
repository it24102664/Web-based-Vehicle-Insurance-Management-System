package com.example.Insurance.controller;

import com.example.Insurance.model.Admin;
import com.example.Insurance.model.User;
import com.example.Insurance.service.AdminService;
import com.example.Insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    // USER REGISTRATION
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    // USER LOGIN
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            boolean isAuthenticated = userService.authenticateUser(email, password);

            if (isAuthenticated) {
                // Store user in session
                User user = userService.findByEmail(email).orElse(null);
                if (user != null) {
                    session.setAttribute("loggedInUser", user);
                    session.setAttribute("userRole", "USER");
                    return "redirect:/dashboard";
                }
            }

            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Login failed: " + e.getMessage());
            return "redirect:/login";
        }
    }

    // ADMIN LOGIN
    @GetMapping("/admin-login")
    public String showAdminLoginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid admin credentials");
        }
        return "admin-login";
    }

    @PostMapping("/admin-login")
    public String processAdminLogin(@RequestParam String email,
                                    @RequestParam String password,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            boolean isAuthenticated = adminService.authenticateAdmin(email, password);

            if (isAuthenticated) {
                // Store admin in session
                Admin admin = adminService.findByEmail(email).orElse(null);
                if (admin != null) {
                    session.setAttribute("loggedInUser", admin);
                    session.setAttribute("userRole", "ADMIN");
                    return "redirect:/admin-dashboard";
                }
            }

            redirectAttributes.addFlashAttribute("error", "Invalid admin credentials");
            return "redirect:/admin-login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Admin login failed: " + e.getMessage());
            return "redirect:/admin-login";
        }
    }

    // ADMIN REGISTRATION
    @GetMapping("/admin-register")
    public String showAdminRegistrationForm(Model model) {
        model.addAttribute("admin", new Admin());
        return "admin-register";
    }

    @PostMapping("/admin-register")
    public String registerAdmin(@ModelAttribute Admin admin, RedirectAttributes redirectAttributes) {
        try {
            adminService.createAdmin(admin);
            redirectAttributes.addFlashAttribute("success", "Admin registration successful! Please login.");
            return "redirect:/admin-login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin-register";
        }
    }

    // LOGOUT
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully");
        return "redirect:/login";
    }

    // DASHBOARD ROUTES
    @GetMapping("/dashboard")
    public String userDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "dashboard";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");

        if (admin == null || !"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }

        // Load dashboard data
        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("users", userService.findAllUsers());

        return "admin-dashboard";
    }
}
