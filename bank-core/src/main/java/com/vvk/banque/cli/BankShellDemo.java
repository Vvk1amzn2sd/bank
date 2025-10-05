package com.vvk.banque.cli;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;

public final class BankShellDemo {
    private static final BankShell core = new BankShell(); 

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080; 
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", BankShellDemo::serveHtml); 
        server.createContext("/cli", BankShellDemo::handleCli); 
        server.start();
        System.out.println("Banque-VVK Production Demo at http://0.0.0.0:" + port);
    }
    
    private static void serveHtml(HttpExchange ex) throws IOException {
        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Banque-VVK | Premium Banking Experience</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #8a2be2;
            --secondary: #00bfff;
            --accent: #ff00ff;
            --glass-bg: rgba(30, 20, 40, 0.25);
            --glass-border: rgba(255, 255, 255, 0.1);
            --text-primary: #e0d6ff;
            --text-secondary: #a090c0;
            --success: #00ff9d;
            --error: #ff4d94;
            --transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
            color: var(--text-primary);
            min-height: 100vh;
            overflow-x: hidden;
            position: relative;
        }

        .space-bg {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: -1;
            overflow: hidden;
        }

        .star {
            position: absolute;
            background: white;
            border-radius: 50%;
            animation: twinkle var(--duration, 5s) infinite ease-in-out;
            opacity: var(--opacity, 0.7);
        }

        .nebula {
            position: absolute;
            border-radius: 50%;
            filter: blur(60px);
            opacity: 0.15;
            animation: float 30s infinite linear;
        }

        @keyframes twinkle {
            0%, 100% { opacity: 0.2; transform: scale(0.8); }
            50% { opacity: 1; transform: scale(1.2); }
        }

        @keyframes float {
            0% { transform: translate(0, 0); }
            25% { transform: translate(20%, 10%); }
            50% { transform: translate(0, 20%); }
            75% { transform: translate(-20%, 10%); }
            100% { transform: translate(0, 0); }
        }

        .glass {
            background: var(--glass-bg);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid var(--glass-border);
            border-radius: 20px;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
            position: relative;
            overflow: hidden;
        }

        .glass::before {
            content: '';
            position: absolute;
            top: -50%;
            left: -50%;
            width: 200%;
            height: 200%;
            background: radial-gradient(circle, rgba(138, 43, 226, 0.1) 0%, transparent 70%);
            z-index: -1;
            animation: rotate 20s linear infinite;
        }

        @keyframes rotate {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
        }

        header {
            padding: 2rem 5%;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .logo {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-icon {
            width: 50px;
            height: 50px;
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 24px;
            box-shadow: 0 0 20px rgba(138, 43, 226, 0.5);
        }

        .logo-text {
            font-size: 1.8rem;
            font-weight: 700;
            background: linear-gradient(to right, var(--primary), var(--secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            letter-spacing: 1px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 2rem;
        }

        .hero {
            text-align: center;
            padding: 3rem 0;
        }

        .hero h1 {
            font-size: 3.5rem;
            margin-bottom: 1rem;
            background: linear-gradient(to right, #e0d6ff, #a090c0);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            text-shadow: 0 0 20px rgba(138, 43, 226, 0.3);
        }

        .hero p {
            font-size: 1.2rem;
            color: var(--text-secondary);
            max-width: 600px;
            margin: 0 auto 2rem;
        }

        .actions-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
            margin: 3rem 0;
        }

        .action-card {
            padding: 2rem;
            transition: var(--transition);
            cursor: pointer;
        }

        .action-card:hover {
            transform: translateY(-10px);
            box-shadow: 0 12px 40px rgba(138, 43, 226, 0.4);
        }

        .action-card h3 {
            font-size: 1.8rem;
            margin-bottom: 1.5rem;
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .action-card i {
            font-size: 2rem;
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .action-form {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }

        .input-group {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
        }

        .input-group label {
            font-size: 0.9rem;
            color: var(--text-secondary);
        }

        .input-group input, .input-group select {
            padding: 0.8rem 1.2rem;
            background: rgba(20, 15, 30, 0.4);
            border: 1px solid var(--glass-border);
            border-radius: 12px;
            color: var(--text-primary);
            font-size: 1rem;
            outline: none;
            transition: var(--transition);
        }

        .input-group input:focus, .input-group select:focus {
            border-color: var(--secondary);
            box-shadow: 0 0 0 3px rgba(0, 191, 255, 0.3);
        }

        .btn {
            padding: 0.9rem 1.5rem;
            border: none;
            border-radius: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: var(--transition);
            text-align: center;
            font-size: 1rem;
        }

        .btn-primary {
            background: linear-gradient(135deg, var(--primary), var(--secondary));
            color: white;
        }

        .btn-primary:hover {
            transform: scale(1.05);
            box-shadow: 0 0 20px rgba(138, 43, 226, 0.6);
        }

        .account-info {
            margin: 3rem 0;
            padding: 2rem;
        }

        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
            margin-top: 1.5rem;
        }

        .info-card {
            padding: 1.5rem;
        }

        .info-card h3 {
            margin-bottom: 1rem;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .balance-display {
            font-size: 2.5rem;
            font-weight: 700;
            margin: 1rem 0;
            color: var(--success);
        }

        .history-list {
            list-style: none;
            max-height: 200px;
            overflow-y: auto;
            padding-right: 10px;
        }

        .history-list li {
            padding: 0.8rem 0;
            border-bottom: 1px solid var(--glass-border);
            display: flex;
            justify-content: space-between;
        }

        .history-list li:last-child {
            border-bottom: none;
        }

        .modal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(10, 5, 20, 0.8);
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            visibility: hidden;
            transition: var(--transition);
            z-index: 1000;
        }

        .modal.active {
            opacity: 1;
            visibility: visible;
        }

        .modal-content {
            width: 90%;
            max-width: 500px;
            padding: 2.5rem;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
        }

        .close-modal {
            background: none;
            border: none;
            color: var(--text-secondary);
            font-size: 1.5rem;
            cursor: pointer;
            transition: var(--transition);
        }

        .close-modal:hover {
            color: var(--error);
        }

        .cli-terminal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(5, 5, 15, 0.95);
            display: flex;
            flex-direction: column;
            padding: 2rem;
            opacity: 0;
            visibility: hidden;
            transform: translateY(100%);
            transition: var(--transition);
            z-index: 2000;
        }

        .cli-terminal.active {
            opacity: 1;
            visibility: visible;
            transform: translateY(0);
        }

        .terminal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid var(--glass-border);
        }

        .terminal-title {
            font-size: 1.5rem;
            color: var(--secondary);
        }

        .terminal-content {
            flex: 1;
            overflow-y: auto;
            padding: 1rem;
            font-family: 'Courier New', monospace;
            line-height: 1.6;
            color: #0f0;
        }

        .terminal-input {
            display: flex;
            margin-top: 1rem;
        }

        .terminal-input span {
            color: var(--secondary);
            margin-right: 10px;
        }

        .terminal-input input {
            background: transparent;
            border: none;
            color: #0f0;
            font-family: 'Courier New', monospace;
            font-size: 1.1rem;
            width: 100%;
            outline: none;
        }

        .terminal-output {
            margin-bottom: 1rem;
            white-space: pre-wrap;
        }

        .command-history {
            color: #0f0;
        }

        .command-output {
            color: #0ff;
        }

        footer {
            text-align: center;
            padding: 2rem;
            margin-top: 3rem;
        }

        .copyright {
            font-size: 0.9rem;
            color: var(--text-secondary);
            cursor: pointer;
            transition: var(--transition);
        }

        .copyright:hover {
            color: var(--accent);
            text-shadow: 0 0 10px var(--accent);
        }

        @media (max-width: 768px) {
            .hero h1 {
                font-size: 2.5rem;
            }
            
            .actions-grid {
                grid-template-columns: 1fr;
            }
            
            .info-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
    <div class="space-bg" id="spaceBg"></div>
    
    <header>
        <div class="logo">
            <div class="logo-icon">V</div>
            <div class="logo-text">Banque-VVK</div>
        </div>
    </header>
    
    <div class="container">
        <div class="hero">
            <h1>Welcome to Banque-VVK</h1>
            <p>Experience the future of banking with our surreal glassmorphism interface. Secure, elegant, and intuitive.</p>
        </div>
        
        <div class="actions-grid">
            <div class="glass action-card" id="depositCard">
                <h3><i class="fas fa-plus-circle"></i> Deposit Funds</h3>
                <form class="action-form" id="depositForm">
                    <div class="input-group">
                        <label for="depositAccount">Account ID</label>
                        <input type="text" id="depositAccount" placeholder="12345" required>
                    </div>
                    <div class="input-group">
                        <label for="depositAmount">Amount</label>
                        <input type="number" id="depositAmount" placeholder="50.00" step="0.01" min="0.01" required>
                    </div>
                    <div class="input-group">
                        <label for="depositCurrency">Currency</label>
                        <select id="depositCurrency">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                            <option value="JPY">JPY</option>
                            <option value="CAD">CAD</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary">Deposit Funds</button>
                </form>
            </div>
            
            <div class="glass action-card" id="withdrawCard">
                <h3><i class="fas fa-minus-circle"></i> Withdraw Funds</h3>
                <form class="action-form" id="withdrawForm">
                    <div class="input-group">
                        <label for="withdrawAccount">Account ID</label>
                        <input type="text" id="withdrawAccount" placeholder="12345" required>
                    </div>
                    <div class="input-group">
                        <label for="withdrawAmount">Amount</label>
                        <input type="number" id="withdrawAmount" placeholder="50.00" step="0.01" min="0.01" required>
                    </div>
                    <div class="input-group">
                        <label for="withdrawCurrency">Currency</label>
                        <select id="withdrawCurrency">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                            <option value="JPY">JPY</option>
                            <option value="CAD">CAD</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary">Withdraw Funds</button>
                </form>
            </div>
            
            <div class="glass action-card" id="transferCard">
                <h3><i class="fas fa-exchange-alt"></i> Transfer Funds</h3>
                <form class="action-form" id="transferForm">
                    <div class="input-group">
                        <label for="fromAccount">From Account</label>
                        <input type="text" id="fromAccount" placeholder="12345" required>
                    </div>
                    <div class="input-group">
                        <label for="toAccount">To Account</label>
                        <input type="text" id="toAccount" placeholder="54321" required>
                    </div>
                    <div class="input-group">
                        <label for="transferAmount">Amount</label>
                        <input type="number" id="transferAmount" placeholder="50.00" step="0.01" min="0.01" required>
                    </div>
                    <div class="input-group">
                        <label for="transferCurrency">Currency</label>
                        <select id="transferCurrency">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                            <option value="JPY">JPY</option>
                            <option value="CAD">CAD</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary">Transfer Funds</button>
                </form>
            </div>
        </div>
        
        <div class="glass account-info">
            <h2>Account Information</h2>
            <div class="info-grid">
                <div class="glass info-card">
                    <h3><i class="fas fa-wallet"></i> Check Balance</h3>
                    <form class="action-form" id="balanceForm">
                        <div class="input-group">
                            <label for="balanceAccount">Account ID</label>
                            <input type="text" id="balanceAccount" placeholder="12345" required>
                        </div>
                        <button type="submit" class="btn btn-primary">Check Balance</button>
                    </form>
                    <div class="balance-display" id="balanceDisplay">--.--</div>
                </div>
                
                <div class="glass info-card">
                    <h3><i class="fas fa-history"></i> Transaction History</h3>
                    <ul class="history-list" id="historyList">
                        <li>Loading accounts...</li>
                    </ul>
                </div>
            </div>
        </div>
        
        <div class="glass" style="margin: 3rem 0; padding: 2rem;">
            <h2>Open New Account</h2>
            <p style="margin: 1rem 0; color: var(--text-secondary);">Create a new customer profile and open your first account</p>
            <button class="btn btn-primary" id="openAccountBtn">Open Account</button>
        </div>
    </div>
    
    <div class="modal" id="signupModal">
        <div class="glass modal-content">
            <div class="modal-header">
                <h2>Open New Account</h2>
                <button class="close-modal" id="closeModal">&times;</button>
            </div>
            <form class="action-form" id="signupForm">
                <div class="input-group">
                    <label for="customerId">Customer ID (3 uppercase letters)</label>
                    <input type="text" id="customerId" placeholder="VVK" maxlength="3" required>
                </div>
                <div class="input-group">
                    <label for="customerEmail">Email</label>
                    <input type="email" id="customerEmail" placeholder="you@example.com" required>
                </div>
                <div class="input-group">
                    <label for="customerPassword">Password</label>
                    <input type="password" id="customerPassword" placeholder="••••••••" required>
                </div>
                <div class="input-group">
                    <label for="initialAmount">Initial Deposit</label>
                    <input type="number" id="initialAmount" placeholder="100.00" step="0.01" min="0.01" required>
                </div>
                <div class="input-group">
                    <label for="signupCurrency">Currency</label>
                    <select id="signupCurrency">
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                        <option value="JPY">JPY</option>
                        <option value="CAD">CAD</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary">Create Account</button>
            </form>
        </div>
    </div>
    
    <div class="cli-terminal" id="cliTerminal">
        <div class="terminal-header">
            <div class="terminal-title">Banque-VVK CLI Terminal</div>
            <button class="close-modal" id="closeTerminal">&times;</button>
        </div>
        <div class="terminal-content" id="terminalContent">
            <div class="terminal-output">Welcome to Banque-VVK CLI Terminal</div>
            <div class="terminal-output">Type 'help' for available commands</div>
        </div>
        <div class="terminal-input">
            <span>$</span>
            <input type="text" id="terminalInput" autocomplete="off" autofocus>
        </div>
    </div>
    
    <footer>
        <div class="copyright" id="copyright">© 2025 Banque-VVK.com</div>
    </footer>

    <script>
        function createBackground() {
            const spaceBg = document.getElementById('spaceBg');
            const colors = ['#8a2be2', '#00bfff', '#ff00ff', '#00ff9d'];
            
            for (let i = 0; i < 150; i++) {
                const star = document.createElement('div');
                star.classList.add('star');
                star.style.left = `${Math.random() * 100}%`;
                star.style.top = `${Math.random() * 100}%`;
                star.style.width = `${Math.random() * 3}px`;
                star.style.height = star.style.width;
                star.style.opacity = Math.random() * 0.8 + 0.2;
                star.style.setProperty('--duration', `${Math.random() * 5 + 3}s`);
                spaceBg.appendChild(star);
            }
            
            for (let i = 0; i < 5; i++) {
                const nebula = document.createElement('div');
                nebula.classList.add('nebula');
                nebula.style.width = `${Math.random() * 300 + 200}px`;
                nebula.style.height = nebula.style.width;
                nebula.style.left = `${Math.random() * 100}%`;
                nebula.style.top = `${Math.random() * 100}%`;
                nebula.style.background = colors[Math.floor(Math.random() * colors.length)];
                nebula.style.setProperty('--duration', `${Math.random() * 20 + 20}s`);
                spaceBg.appendChild(nebula);
            }
        }
        
        async function sendCommand(command) {
            try {
                const response = await fetch('/cli', {
                    method: 'POST',
                    body: command
                });
                const result = await response.text();
                return result;
            } catch (error) {
                console.error('Error sending command:', error);
                return `Error: ${error.message}`;
            }
        }
        
        document.getElementById('depositForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const account = document.getElementById('depositAccount').value;
            const amount = document.getElementById('depositAmount').value;
            const currency = document.getElementById('depositCurrency').value;
            const command = `deposit ${account} ${amount} ${currency}`;
            
            const result = await sendCommand(command);
            alert(result);
            document.getElementById('depositForm').reset();
        });
        
        document.getElementById('withdrawForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const account = document.getElementById('withdrawAccount').value;
            const amount = document.getElementById('withdrawAmount').value;
            const currency = document.getElementById('withdrawCurrency').value;
            const command = `withdraw ${account} ${amount} ${currency}`;
            
            const result = await sendCommand(command);
            alert(result);
            document.getElementById('withdrawForm').reset();
        });
        
        document.getElementById('transferForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const from = document.getElementById('fromAccount').value;
            const to = document.getElementById('toAccount').value;
            const amount = document.getElementById('transferAmount').value;
            const currency = document.getElementById('transferCurrency').value;
            const command = `transfer ${from} ${to} ${amount} ${currency}`;
            
            const result = await sendCommand(command);
            alert(result);
            document.getElementById('transferForm').reset();
        });
        
        document.getElementById('balanceForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const account = document.getElementById('balanceAccount').value;
            const command = `balance ${account}`;
            
            const result = await sendCommand(command);
            document.getElementById('balanceDisplay').textContent = result;
            document.getElementById('balanceForm').reset();
        });
        
        document.getElementById('signupForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const customerId = document.getElementById('customerId').value.toUpperCase();
            const email = document.getElementById('customerEmail').value;
            const password = document.getElementById('customerPassword').value;
            const amount = document.getElementById('initialAmount').value;
            const currency = document.getElementById('signupCurrency').value;
            
            const signupCommand = `signup ${customerId} ${email} ${password}`;
            let result = await sendCommand(signupCommand);
            
            if (!result.includes('Error')) {
                const openCommand = `open ${customerId} ${amount} ${currency}`;
                result = await sendCommand(openCommand);
            }
            
            alert(result);
            document.getElementById('signupForm').reset();
            document.getElementById('signupModal').classList.remove('active');
        });
        
        document.getElementById('openAccountBtn').addEventListener('click', () => {
            document.getElementById('signupModal').classList.add('active');
        });
        
        document.getElementById('closeModal').addEventListener('click', () => {
            document.getElementById('signupModal').classList.remove('active');
        });
        
        const terminalContent = document.getElementById('terminalContent');
        const terminalInput = document.getElementById('terminalInput');
        const cliTerminal = document.getElementById('cliTerminal');
        const closeTerminal = document.getElementById('closeTerminal');
        
        const commandMap = {
            '1': 'signup',
            '2': 'open',
            '3': 'deposit',
            '4': 'withdraw',
            '5': 'transfer',
            '6': 'balance',
            '7': 'vvk_list',
            '8': 'vvk_settle',
            '9': 'exit'
        };
        
        function showHelp() {
            const helpText = `
1. signup   <3LETTERS> <email> <pwd>          - create customer (e.g.: signup VVK ixnine@amzn.to)
2. open     <3LETTERS> <amount> [CUR]         - create account (e.g.: open VVK 100 USD or open VVK 100 [defaults to USD])
3. deposit  <5DIGITS> <amount> [CUR]          - add money (e.g.: deposit 12345 50 USD)
4. withdraw <5DIGITS> <amount> [CUR]          - take money (e.g.: withdraw 12345 30 USD)
5. transfer <5DIGITS> <5DIGITS> <amount> [CUR] - move money (e.g.: transfer 12345 54321 20 USD)
6. balance  <5DIGITS>                         - get account balance
7. vvk_list                                   - list all accounts
8. vvk_settle <password>                      - settle pending transfers (only preeya can run, may ask pw from her :-0)
9. exit                                       - quit
            `.trim();
            
            addTerminalOutput(helpText);
        }
        
        function addTerminalOutput(text, isCommand = false) {
            const outputDiv = document.createElement('div');
            outputDiv.className = isCommand ? 'command-history' : 'command-output';
            outputDiv.textContent = text;
            terminalContent.appendChild(outputDiv);
            terminalContent.scrollTop = terminalContent.scrollHeight;
        }
        
        terminalInput.addEventListener('keypress', async (e) => {
            if (e.key === 'Enter') {
                const input = terminalInput.value.trim();
                if (input === '') return;
                
                addTerminalOutput(`$ ${input}`, true);
                terminalInput.value = '';
                
                if (input === 'help') {
                    showHelp();
                    return;
                }
                
                if (input === 'exit') {
                    cliTerminal.classList.remove('active');
                    return;
                }
                
                if (input in commandMap) {
                    const commandName = commandMap[input];
                    addTerminalOutput(`Command: ${commandName}`);
                    addTerminalOutput('Please enter the full command with parameters as shown in help');
                    return;
                }
                
                const result = await sendCommand(input);
                addTerminalOutput(result);
            }
        });
        
        closeTerminal.addEventListener('click', () => {
            cliTerminal.classList.remove('active');
        });
        
        document.getElementById('copyright').addEventListener('dblclick', () => {
            cliTerminal.classList.add('active');
            terminalInput.focus();
            showHelp();
        });
        
        // AUTO-LOAD ACCOUNTS ON STARTUP
        window.addEventListener('load', async () => {
            createBackground();
            const listResult = await sendCommand('vvk_list');
            const historyList = document.getElementById('historyList');
            
            if (listResult.trim() === '' || listResult.includes('Error')) {
                historyList.innerHTML = '<li>No accounts found</li>';
            } else {
                const accounts = listResult.split('\\n').filter(line => line.trim() !== '');
                if (accounts.length === 1 && accounts[0] === '') {
                    historyList.innerHTML = '<li>No accounts found</li>';
                } else {
                    historyList.innerHTML = accounts.map(acc => {
                        const parts = acc.split(' ');
                        if (parts.length >= 5) {
                            const id = parts[0];
                            const cust = parts[1];
                            const balance = parts[parts.length - 2];
                            const currency = parts[parts.length - 1];
                            return `<li><strong>${id}</strong> (${cust})<span>${balance} ${currency}</span></li>`;
                        }
                        return `<li>${acc}</li>`;
                    }).join('');
                }
            }
        });
    </script>
</body>
</html>
""";
        
        ex.sendResponseHeaders(200, html.getBytes().length);
        try (OutputStream os = ex.getResponseBody()) { 
            os.write(html.getBytes()); 
        }
    }

    private static void handleCli(HttpExchange ex) throws IOException {
        String cmd = new String(ex.getRequestBody().readAllBytes()).trim();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf, true);

        PrintStream old = System.out;
        System.setOut(ps);
        try {
            core.handle(cmd);
        }
        catch (Exception e) {
            ps.println("Error: " + e.getMessage());
        }
        finally {
            System.setOut(old);
        }

        // CRITICAL FIX: Convert \n to \r\n for proper line breaks in browser
        String output = buf.toString().replace("\n", "\r\n");
        byte[] out = output.getBytes();
        ex.sendResponseHeaders(200, out.length);
        try (OutputStream os = ex.getResponseBody()) { 
            os.write(out); 
        }
    }
}
